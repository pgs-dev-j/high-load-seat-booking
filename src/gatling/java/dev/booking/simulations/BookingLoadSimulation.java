package dev.booking.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Two scenarios, run against whichever booking strategy is currently active
 * (controlled by the `spring.profiles.active` property on the running app —
 * naive / pessimistic / optimistic / redis). Point this at each stage in turn
 * and compare target/gatling-results across runs.
 *
 * HOT SEAT     — every virtual user fights over the exact same seat.
 *                Measures how the strategy behaves under maximum contention.
 * DISTRIBUTED  — realistic traffic across 200 different seats.
 *                Measures baseline throughput cost of the locking strategy
 *                when there's little to no contention.
 *
 * Configure via -D system properties, e.g.:
 *   mvn gatling:test -Dusers=200 -Dduration=30 -DbaseUrl=http://localhost:8080
 */
public class BookingLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int USERS = Integer.parseInt(System.getProperty("users", "200"));
    private static final int DURATION_SECONDS = Integer.parseInt(System.getProperty("duration", "30"));

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // ---- HOT SEAT SCENARIO ----------------------------------------------
    // Every user is handed the SAME seatId (the first one from the event's
    // seat list) so every request contends for one row.

    ScenarioBuilder hotSeatScenario = scenario("Hot seat contention")
            .exec(
                    http("Fetch seat list")
                            .get("/api/events/1/seats")
                            .check(jsonPath("$[0].id").saveAs("hotSeatId"))
            )
            .exec(
                    http("Book hot seat")
                            .post("/api/bookings")
                            .body(StringBody(session -> """
                                    {"seatId": %s, "userId": "gatling-hot-%s"}
                                    """.formatted(
                                    session.getString("hotSeatId"),
                                    session.userId())))
                            .check(status().in(201, 409)) // both are "expected" outcomes; we care about the RATE
            );

    // ---- DISTRIBUTED SCENARIO --------------------------------------------
    // Each user books a different seat drawn from the full seat list —
    // simulates realistic traffic with little contention.

    ScenarioBuilder distributedScenario = scenario("Distributed booking")
            .exec(
                    http("Fetch seat list")
                            .get("/api/events/1/seats")
                            .check(jsonPath("$[*].id").findAll().saveAs("allSeatIds"))
            )
            .exec(session -> {
                @SuppressWarnings("unchecked")
                var seatIds = (java.util.List<String>) session.get("allSeatIds");
                String chosen = seatIds.get(ThreadLocalRandom.current().nextInt(seatIds.size()));
                return session.set("chosenSeatId", chosen);
            })
            .exec(
                    http("Book distinct seat")
                            .post("/api/bookings")
                            .body(StringBody(session -> """
                                    {"seatId": %s, "userId": "gatling-dist-%s"}
                                    """.formatted(
                                    session.getString("chosenSeatId"),
                                    session.userId())))
                            .check(status().in(201, 409))
            );

    {
        setUp(
                hotSeatScenario.injectOpen(
                        atOnceUsers(USERS)
                ).protocols(httpProtocol),

                distributedScenario.injectOpen(
                        rampUsers(USERS).during(DURATION_SECONDS)
                ).protocols(httpProtocol)
        ).assertions(
                global().responseTime().percentile3().lt(2000),
                global().failedRequests().percent().lt(1.0)
        );
    }
}
