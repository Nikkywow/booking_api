package com.example.hotel.controller;

import com.example.hotel.dto.ConfirmAvailabilityRequest;
import com.example.hotel.dto.ReleaseRoomRequest;
import com.example.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class InternalRoomController {
    private final HotelService hotelService;

    @Value("${internal.service-key}")
    private String internalKey;

    @PostMapping("/{id}/confirm-availability")
    public void confirmAvailability(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Key") String key,
            @RequestBody ConfirmAvailabilityRequest request
    ) {
        validateInternalKey(key);
        hotelService.confirmAvailability(id, request.startDate(), request.endDate(), request.requestId(), request.bookingId());
    }

    @PostMapping("/{id}/release")
    public void release(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Key") String key,
            @RequestBody ReleaseRoomRequest request
    ) {
        validateInternalKey(key);
        hotelService.release(id, request.requestId());
    }

    private void validateInternalKey(String key) {
        if (!internalKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden internal route");
        }
    }
}
