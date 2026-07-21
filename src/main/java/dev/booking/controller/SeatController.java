package dev.booking.controller;

import dev.booking.domain.Seat;
import dev.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class SeatController {

    private final SeatRepository seatRepository;

    @GetMapping("/{eventId}/seats")
    public List<Map<String, Object>> seatsForEvent(@PathVariable Long eventId) {
        return seatRepository.findByEventId(eventId).stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(Seat s) {
        return Map.of(
                "id", s.getId(),
                "seatNumber", s.getSeatNumber(),
                "status", s.getStatus().name()
        );
    }
}
