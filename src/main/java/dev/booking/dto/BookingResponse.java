package dev.booking.dto;

import dev.booking.domain.Booking;

import java.time.Instant;

public record BookingResponse(
        Long bookingId,
        Long seatId,
        String seatNumber,
        String userId,
        Instant bookedAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getSeat().getId(),
                booking.getSeat().getSeatNumber(),
                booking.getUserId(),
                booking.getBookedAt()
        );
    }
}
