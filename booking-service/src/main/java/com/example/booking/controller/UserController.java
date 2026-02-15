package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.entity.UserEntity;
import com.example.booking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/auth")
    public AuthResponse auth(@Valid @RequestBody AuthRequest request) {
        return userService.auth(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserEntity create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserEntity update(@RequestBody UpdateUserRequest request) {
        return userService.update(request);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@RequestParam Long id) {
        userService.delete(id);
    }
}
