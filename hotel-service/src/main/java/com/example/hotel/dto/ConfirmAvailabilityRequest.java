package com.example.hotel.dto;

import java.time.LocalDate;

public record ConfirmAvailabilityRequest(LocalDate startDate, LocalDate endDate, String requestId, Long bookingId) {
}
