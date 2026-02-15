package com.example.hotel.repo;

import com.example.hotel.entity.ReservationStatus;
import com.example.hotel.entity.RoomReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomReservationRepository extends JpaRepository<RoomReservationEntity, Long> {
    Optional<RoomReservationEntity> findByRequestId(String requestId);

    Optional<RoomReservationEntity> findByRoom_IdAndRequestId(Long roomId, String requestId);

    @Query("""
            select count(rr) > 0 from RoomReservationEntity rr
            where rr.room.id = :roomId
            and rr.status = :status
            and rr.startDate < :endDate
            and rr.endDate > :startDate
            """)
    boolean existsOverlapping(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ReservationStatus status
    );

    @Query("""
            select rr.room.id from RoomReservationEntity rr
            where rr.status = :status
            and rr.startDate < :endDate
            and rr.endDate > :startDate
            """)
    List<Long> findBusyRoomIds(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ReservationStatus status
    );
}
