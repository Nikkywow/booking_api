package com.example.hotel.service;

import com.example.hotel.dto.CreateHotelRequest;
import com.example.hotel.dto.CreateRoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.entity.*;
import com.example.hotel.repo.HotelRepository;
import com.example.hotel.repo.RoomRepository;
import com.example.hotel.repo.RoomReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    public HotelEntity createHotel(CreateHotelRequest request) {
        return hotelRepository.save(HotelEntity.builder().name(request.name()).address(request.address()).build());
    }

    public RoomEntity createRoom(CreateRoomRequest request) {
        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found"));
        return roomRepository.save(RoomEntity.builder()
                .hotel(hotel)
                .number(request.number())
                .available(request.available())
                .timesBooked(0)
                .build());
    }

    public List<HotelEntity> allHotels() {
        return hotelRepository.findAll();
    }

    public List<RoomResponse> availableRooms(LocalDate startDate, LocalDate endDate, boolean recommend) {
        List<Long> busyRoomIds = reservationRepository.findBusyRoomIds(startDate, endDate, ReservationStatus.ACTIVE);
        List<RoomResponse> rooms = roomRepository.findAll().stream()
                .filter(RoomEntity::isAvailable)
                .filter(r -> !busyRoomIds.contains(r.getId()))
                .map(r -> new RoomResponse(r.getId(), r.getHotel().getId(), r.getNumber(), r.isAvailable(), r.getTimesBooked()))
                .toList();

        if (!recommend) {
            return rooms;
        }
        return rooms.stream()
                .sorted(Comparator.comparingInt(RoomResponse::timesBooked).thenComparing(RoomResponse::roomId))
                .toList();
    }

    @Transactional
    public void confirmAvailability(Long roomId, LocalDate startDate, LocalDate endDate, String requestId, Long bookingId) {
        RoomReservationEntity existing = reservationRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ReservationStatus.ACTIVE) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already cancelled");
        }

        RoomEntity room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!room.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is unavailable");
        }

        if (reservationRepository.existsOverlapping(roomId, startDate, endDate, ReservationStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked");
        }

        reservationRepository.save(RoomReservationEntity.builder()
                .room(room)
                .requestId(requestId)
                .bookingId(bookingId)
                .startDate(startDate)
                .endDate(endDate)
                .status(ReservationStatus.ACTIVE)
                .build());

        room.setTimesBooked(room.getTimesBooked() + 1);
    }

    @Transactional
    public void release(Long roomId, String requestId) {
        RoomReservationEntity reservation = reservationRepository.findByRoom_IdAndRequestId(roomId, requestId).orElse(null);
        if (reservation == null || reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        RoomEntity room = reservation.getRoom();
        if (room.getTimesBooked() > 0) {
            room.setTimesBooked(room.getTimesBooked() - 1);
        }
    }
}
