package dev.booking.service;

import dev.booking.domain.Booking;
import dev.booking.exception.SeatAlreadyBookedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * STAGE 3 — OPTIMISTIC LOCKING.
 *
 * No lock is held while reading. Instead, every write includes a check
 * against the @Version column the read saw. If another transaction
 * changed the row in between, Hibernate throws
 * ObjectOptimisticLockingFailureException on flush — we catch that here
 * and retry with a FRESH read (the previous attempt's stale entity is
 * useless, retrying must start over from bookAttempt.tryBook, not resume
 * mid-transaction).
 *
 * Trade-off to measure: under LOW contention this should be the cheapest
 * strategy (no locking overhead at all on the happy path). Under the
 * hot-seat burst scenario, expect the OPPOSITE of pessimistic locking's
 * queue behavior — most of the 200 requests will conflict on their first
 * attempt and burn CPU/DB round-trips retrying, likely several times
 * each, before giving up or succeeding. Watch retry counts, not just
 * latency, in the Gatling comparison.
 */
@Service
@Profile("optimistic")
@RequiredArgsConstructor
public class OptimisticLockBookingService implements BookingService {

    private static final int MAX_RETRIES = 10;

    private final OptimisticBookingAttempt bookingAttempt;

    @Override
    public Booking bookSeat(Long seatId, String userId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return bookingAttempt.tryBook(seatId, userId);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt == MAX_RETRIES) {
                    throw new SeatAlreadyBookedException(seatId);
                }
            }
        }
        throw new IllegalStateException("Unreachable");
    }
}
