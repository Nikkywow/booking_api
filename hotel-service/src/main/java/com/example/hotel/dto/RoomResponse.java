package com.example.hotel.dto;

public record RoomResponse(Long roomId, Long hotelId, String number, boolean available, int timesBooked) {
}
