package dev.booking.service;

import dev.booking.domain.Booking;
import dev.booking.domain.Seat;
import dev.booking.domain.SeatStatus;
import dev.booking.exception.SeatAlreadyBookedException;
import dev.booking.exception.SeatNotFoundException;
import dev.booking.repository.BookingRepository;
import dev.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OptimisticBookingAttempt {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public Booking tryBook(Long seatId, String userId) {
        Seat seat = seatRepository.findById(seatId)
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
