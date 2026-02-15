package com.example.booking.repo;

import com.example.booking.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    Optional<BookingEntity> findByRequestId(String requestId);
    List<BookingEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
