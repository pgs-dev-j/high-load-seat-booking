package dev.booking.config;

import dev.booking.domain.Event;
import dev.booking.domain.Seat;
import dev.booking.repository.EventRepository;
import dev.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds one event with 200 seats on startup, if the DB is empty.
 * Seat "A1" is the designated "hot seat" used in load tests where
 * many virtual users compete for the exact same seat.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            return;
        }

        Event event = new Event();
        event.setName("High-Load Arena Concert");
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event = eventRepository.save(event);

        for (int row = 1; row <= 20; row++) {
            for (int col = 1; col <= 10; col++) {
                String seatNumber = "R" + row + "-S" + col;
                seatRepository.save(new Seat(event, seatNumber));
            }
        }
    }
}
