package dev.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookSeatRequest(
        @NotNull Long seatId,
        @NotBlank String userId
) {
}
