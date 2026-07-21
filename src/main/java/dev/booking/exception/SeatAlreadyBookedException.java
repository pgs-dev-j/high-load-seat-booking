package dev.booking.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(Long seatId) {
        super("Seat " + seatId + " is already booked");
    }
}
