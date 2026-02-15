package com.example.booking.dto;

import com.example.booking.entity.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long userId,
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        String requestId,
        Instant createdAt
) {
}
