package com.example.booking.dto;

import com.example.booking.entity.Role;

public record UpdateUserRequest(
        Long id,
        String username,
        String password,
        Role role
) {
}
