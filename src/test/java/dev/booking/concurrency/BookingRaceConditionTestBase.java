package dev.booking.concurrency;

import dev.booking.domain.Event;
import dev.booking.domain.Seat;
import dev.booking.repository.BookingRepository;
import dev.booking.repository.EventRepository;
import dev.booking.repository.SeatRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Shared race-condition test harness for ALL booking strategies.
 *
 * Fires CONCURRENT_REQUESTS truly-simultaneous booking requests (via
 * CyclicBarrier, not just "roughly around the same time") at the exact
 * same seat and asserts only one can ever succeed.
 *
 * On the naive strategy (Stage 1) this test is EXPECTED TO FAIL — that
 * failure is the automated proof of the bug. On every subsequent strategy
 * (pessimistic, optimistic, redis...) it is EXPECTED TO PASS with zero
 * changes to this file — only the @ActiveProfiles on the concrete subclass
 * changes. That "same test, different profile, red-to-green" story is the
 * whole point of structuring it this way — don't duplicate this logic
 * per stage, just extend it.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BookingRaceConditionTestBase {

    private static final int CONCURRENT_REQUESTS = 50;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booking")
            .withUsername("booking")
            .withPassword("booking");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    BookingRepository bookingRepository;

    Long contestedSeatId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        Event event = new Event();
        event.setName("Race Condition Test Event");
        event.setEventDate(Instant.now());
        event = eventRepository.save(event);

        Seat seat = new Seat(event, "HOT-SEAT-1");
        seat = seatRepository.save(seat);
        contestedSeatId = seat.getId();
    }

    @Test
    void onlyOneBookingShouldSucceedForTheSameSeat() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CyclicBarrier startingGun = new CyclicBarrier(CONCURRENT_REQUESTS);
        CountDownLatch done = new CountDownLatch(CONCURRENT_REQUESTS);

        AtomicInteger httpSuccessCount = new AtomicInteger(0);
        AtomicInteger httpConflictCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final String userId = "user-" + i;
            pool.submit(() -> {
                try {
                    startingGun.await(); // line every thread up, release together
                    int status = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body("""
                                {"seatId": %d, "userId": "%s"}
                                """.formatted(contestedSeatId, userId))
                            .post("/api/bookings")
                            .statusCode();

                    if (status == 201) {
                        httpSuccessCount.incrementAndGet();
                    } else if (status == 409) {
                        httpConflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // connection errors etc. — counted as neither success nor conflict
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        long actualBookingRows = bookingRepository.countBySeatId(contestedSeatId);

        System.out.printf(
                "%n=== RACE CONDITION TEST RESULT (%s) ===%n" +
                "HTTP 201 (success) responses : %d%n" +
                "HTTP 409 (conflict) responses : %d%n" +
                "Actual Booking rows in DB     : %d%n" +
                "===================================%n%n",
                getClass().getSimpleName(),
                httpSuccessCount.get(), httpConflictCount.get(), actualBookingRows);

        assertEquals(1, actualBookingRows,
                "Expected exactly one booking to succeed for the contested seat, " +
                "but got " + actualBookingRows + ".");
    }
}
