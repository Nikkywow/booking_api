package com.example.booking.service;

import com.example.booking.client.HotelClient;
import com.example.booking.dto.*;
import com.example.booking.entity.BookingEntity;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.UserEntity;
import com.example.booking.repo.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final HotelClient hotelClient;

    @Transactional
    public BookingResponse create(String username, BookingCreateRequest request) {
        if (request.endDate().isBefore(request.startDate()) || request.endDate().equals(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        String requestId = request.requestId() == null || request.requestId().isBlank()
                ? UUID.randomUUID().toString()
                : request.requestId();

        BookingEntity existing = bookingRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        UserEntity user = userService.findByUsernameOrThrow(username);

        Long roomId = request.roomId();
        if (request.autoSelect()) {
            roomId = hotelClient.recommend(request.startDate(), request.endDate()).stream()
                    .findFirst()
                    .map(RoomRecommendationDto::roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No rooms available"));
        }

        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required when autoSelect=false");
        }

        BookingEntity booking = bookingRepository.save(BookingEntity.builder()
                .userId(user.getId())
                .roomId(roomId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(BookingStatus.PENDING)
                .createdAt(Instant.now())
                .requestId(requestId)
                .build());

        log.info("bookingId={} requestId={} status=PENDING", booking.getId(), requestId);

        try {
            hotelClient.confirmAvailability(roomId,
                    new ConfirmAvailabilityRequest(booking.getStartDate(), booking.getEndDate(), requestId, booking.getId()));
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("bookingId={} requestId={} status=CONFIRMED", booking.getId(), requestId);
        } catch (Exception ex) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.warn("bookingId={} requestId={} status=CANCELLED reason={}", booking.getId(), requestId, ex.getMessage());
            try {
                hotelClient.release(roomId, new ReleaseRoomRequest(requestId, booking.getId()));
                log.info("bookingId={} requestId={} compensation=RELEASED", booking.getId(), requestId);
            } catch (Exception releaseEx) {
                log.error("bookingId={} requestId={} compensation_failed={}", booking.getId(), requestId, releaseEx.getMessage());
            }

            if (ex instanceof WebClientResponseException conflict && conflict.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not available");
            }
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Hotel service is unavailable");
        }

        return toResponse(booking);
    }

    public List<BookingResponse> history(String username) {
        Long userId = userService.findByUsernameOrThrow(username).getId();
        return bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    public BookingResponse getById(String username, Long id) {
        UserEntity user = userService.findByUsernameOrThrow(username);
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUserId().equals(user.getId()) && user.getRole() != com.example.booking.entity.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(String username, Long id) {
        UserEntity user = userService.findByUsernameOrThrow(username);
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUserId().equals(user.getId()) && user.getRole() != com.example.booking.entity.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        try {
            hotelClient.release(booking.getRoomId(), new ReleaseRoomRequest(booking.getRequestId(), booking.getId()));
            log.info("bookingId={} requestId={} cancelled_and_released", booking.getId(), booking.getRequestId());
        } catch (Exception ex) {
            log.warn("bookingId={} requestId={} release_error={}", booking.getId(), booking.getRequestId(), ex.getMessage());
        }
        return toResponse(booking);
    }

    private BookingResponse toResponse(BookingEntity e) {
        return new BookingResponse(
                e.getId(),
                e.getUserId(),
                e.getRoomId(),
                e.getStartDate(),
                e.getEndDate(),
                e.getStatus(),
                e.getRequestId(),
                e.getCreatedAt()
        );
    }
}
