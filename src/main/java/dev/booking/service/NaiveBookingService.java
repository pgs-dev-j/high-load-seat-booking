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
 * STAGE 1 — NAIVE IMPLEMENTATION.
 *
 * This is deliberately broken under concurrency. It does a plain
 * read-check-write with no pessimistic lock, no @Version check, and no
 * external lock. Two threads can both read the seat as AVAILABLE before
 * either commits its write — classic lost-update / double-booking.
 *
 * Kept in the codebase on purpose as the baseline the other strategies
 * are benchmarked against. Do not "fix" this class — fix it by adding a
 * new strategy (see PessimisticLockBookingService, OptimisticLockBookingService,
 * RedisLockBookingService in later stages) and comparing results.
 */
@Service
@Profile("naive")
@RequiredArgsConstructor
public class NaiveBookingService implements BookingService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Booking bookSeat(Long seatId, String userId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // <-- race window: two concurrent transactions can both pass this
        //     check before either one's UPDATE is committed.
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new SeatAlreadyBookedException(seatId);
        }

        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        Booking booking = new Booking(seat, userId);
        return bookingRepository.save(booking);
    }
}
