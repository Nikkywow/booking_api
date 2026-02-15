package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateHotelRequest(@NotBlank String name, @NotBlank String address) {
}
