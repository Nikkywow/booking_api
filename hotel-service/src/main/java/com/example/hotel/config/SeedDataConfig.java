package com.example.hotel.config;

import com.example.hotel.entity.HotelEntity;
import com.example.hotel.entity.RoomEntity;
import com.example.hotel.repo.HotelRepository;
import com.example.hotel.repo.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedData(HotelRepository hotelRepository, RoomRepository roomRepository) {
        return args -> {
            if (hotelRepository.count() == 0) {
                HotelEntity hotel = hotelRepository.save(HotelEntity.builder()
                        .name("Central Hotel")
                        .address("Main St 1")
                        .build());

                roomRepository.save(RoomEntity.builder().hotel(hotel).number("101").available(true).timesBooked(0).build());
                roomRepository.save(RoomEntity.builder().hotel(hotel).number("102").available(true).timesBooked(0).build());
            }
        };
    }
}
