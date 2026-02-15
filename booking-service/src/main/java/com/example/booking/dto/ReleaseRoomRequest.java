package com.example.booking.dto;

public record ReleaseRoomRequest(
        String requestId,
        Long bookingId
) {
}
