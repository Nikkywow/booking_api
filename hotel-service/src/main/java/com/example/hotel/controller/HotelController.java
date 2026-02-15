package com.example.hotel.controller;

import com.example.hotel.dto.CreateHotelRequest;
import com.example.hotel.dto.CreateRoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.entity.HotelEntity;
import com.example.hotel.entity.RoomEntity;
import com.example.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;

    @PostMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelEntity createHotel(@Valid @RequestBody CreateHotelRequest request) {
        return hotelService.createHotel(request);
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomEntity createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return hotelService.createRoom(request);
    }

    @GetMapping("/hotels")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<HotelEntity> allHotels() {
        return hotelService.allHotels();
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<RoomResponse> availableRooms(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return hotelService.availableRooms(startDate, endDate, false);
    }

    @GetMapping("/rooms/recommend")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<RoomResponse> recommendRooms(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return hotelService.availableRooms(startDate, endDate, true);
    }
}
