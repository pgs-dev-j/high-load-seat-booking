package dev.booking.service;

import dev.booking.domain.Booking;

public interface BookingService {

    /**
     * Attempts to book a seat for a user.
     *
     * @throws dev.booking.exception.SeatNotFoundException if the seat doesn't exist
     * @throws dev.booking.exception.SeatAlreadyBookedException if the seat is taken
     */
    Booking bookSeat(Long seatId, String userId);
}
