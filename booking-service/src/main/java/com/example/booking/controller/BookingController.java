package com.example.booking.controller;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookingCreateRequest request) {
        return bookingService.create(jwt.getSubject(), request);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<BookingResponse> history(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.history(jwt.getSubject());
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.getById(jwt.getSubject(), id);
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(jwt.getSubject(), id);
    }
}
