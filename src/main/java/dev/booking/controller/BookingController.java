package dev.booking.controller;

import dev.booking.dto.BookSeatRequest;
import dev.booking.dto.BookingResponse;
import dev.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody BookSeatRequest request) {
        var booking = bookingService.bookSeat(request.seatId(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }
}
