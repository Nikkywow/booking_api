package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingCreateRequest(
        Long roomId,
        boolean autoSelect,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String requestId
) {
}
