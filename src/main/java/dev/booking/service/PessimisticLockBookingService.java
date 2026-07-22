package dev.booking.service;

import dev.booking.domain.Booking;
import dev.booking.domain.Seat;
import dev.booking.domain.SeatStatus;
import dev.booking.exception.SeatAlreadyBookedException;
import dev.booking.exception.SeatNotFoundException;
import dev.booking.repository.BookingRepository;
import dev.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * STAGE 2 — PESSIMISTIC LOCKING.
 *
 * Fixes the naive race by taking an exclusive row lock at READ time
 * (SELECT ... FOR UPDATE), not just implicitly at write time like Postgres
 * already does for plain UPDATE. Every other transaction trying to read
 * this same seat with FOR UPDATE now blocks until the first one commits
 * or rolls back — closing the exact window NaiveBookingService left open.
 *
 * Trade-off to measure and write about: correctness is now guaranteed,
 * but every concurrent request for the SAME seat is fully serialized —
 * expect hot-seat latency to scale roughly linearly with contention,
 * while distinct-seat latency should stay close to the Stage 1 baseline
 * (different rows never contend for the same lock).
 */
@Service
@Profile("pessimistic")
@RequiredArgsConstructor
public class PessimisticLockBookingService implements BookingService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Booking bookSeat(Long seatId, String userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new SeatAlreadyBookedException(seatId);
        }

        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        Booking booking = new Booking(seat, userId);
        return bookingRepository.save(booking);
    }
}
