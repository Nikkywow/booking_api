package com.example.booking;

import com.example.booking.client.HotelClient;
import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.entity.BookingEntity;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Role;
import com.example.booking.entity.UserEntity;
import com.example.booking.repo.BookingRepository;
import com.example.booking.service.BookingService;
import com.example.booking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceWorkflowTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserService userService;
    @Mock
    private HotelClient hotelClient;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, userService, hotelClient);
        when(userService.findByUsernameOrThrow("u1")).thenReturn(UserEntity.builder().id(1L).username("u1").role(Role.USER).build());
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> {
            BookingEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(100L);
            }
            return entity;
        });
    }

    @Test
    void createBookingSuccess() {
        when(bookingRepository.findByRequestId("r1")).thenReturn(Optional.empty());

        BookingResponse response = bookingService.create("u1", new BookingCreateRequest(
                10L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "r1"
        ));

        assertEquals(BookingStatus.CONFIRMED, response.status());
        verify(hotelClient, times(1)).confirmAvailability(eq(10L), any());
        verify(hotelClient, never()).release(anyLong(), any());
    }

    @Test
    void createBookingConflictCompensates() {
        when(bookingRepository.findByRequestId("r2")).thenReturn(Optional.empty());
        doThrow(WebClientResponseException.create(409, "Conflict", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .when(hotelClient).confirmAvailability(eq(10L), any());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create("u1", new BookingCreateRequest(
                        10L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "r2"
                )));

        assertEquals(409, ex.getStatusCode().value());
        verify(hotelClient, times(1)).release(eq(10L), any());
    }

    @Test
    void createBookingTimeoutCompensates() {
        when(bookingRepository.findByRequestId("r3")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("timeout")).when(hotelClient).confirmAvailability(eq(10L), any());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create("u1", new BookingCreateRequest(
                        10L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "r3"
                )));

        assertEquals(504, ex.getStatusCode().value());
        verify(hotelClient, times(1)).release(eq(10L), any());
    }

    @Test
    void createBookingIdempotentRedelivery() {
        BookingEntity existing = BookingEntity.builder()
                .id(777L)
                .userId(1L)
                .roomId(10L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .status(BookingStatus.CONFIRMED)
                .requestId("r4")
                .createdAt(java.time.Instant.now())
                .build();

        when(bookingRepository.findByRequestId("r4")).thenReturn(Optional.of(existing));

        BookingResponse response = bookingService.create("u1", new BookingCreateRequest(
                10L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "r4"
        ));

        assertEquals(777L, response.id());
        verifyNoInteractions(hotelClient);
    }
}
