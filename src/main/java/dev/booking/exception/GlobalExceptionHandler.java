package dev.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyBooked(SeatAlreadyBookedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "SEAT_ALREADY_BOOKED",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(SeatNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(SeatNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "SEAT_NOT_FOUND",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
