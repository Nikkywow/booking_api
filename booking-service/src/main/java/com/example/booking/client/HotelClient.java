package com.example.booking.client;

import com.example.booking.dto.ConfirmAvailabilityRequest;
import com.example.booking.dto.ReleaseRoomRequest;
import com.example.booking.dto.RoomRecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HotelClient {
    private final WebClient webClient;

    @Value("${hotel.service.base-url}")
    private String baseUrl;

    @Value("${hotel.service.timeout-ms}")
    private long timeoutMs;

    @Value("${hotel.service.retries}")
    private int retries;

    @Value("${hotel.service.backoff-ms}")
    private long backoffMs;

    @Value("${hotel.service.internal-key}")
    private String internalKey;

    public List<RoomRecommendationDto> recommend(LocalDate startDate, LocalDate endDate) {
        RoomRecommendationDto[] rooms = webClient.get()
                .uri(baseUrl + "/api/rooms/recommend?startDate={start}&endDate={end}", startDate, endDate)
                .retrieve()
                .bodyToMono(RoomRecommendationDto[].class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(retrySpec())
                .block();
        return rooms == null ? List.of() : Arrays.asList(rooms);
    }

    public void confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        webClient.post()
                .uri(baseUrl + "/api/rooms/{id}/confirm-availability", roomId)
                .header("X-Internal-Key", internalKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(retrySpec())
                .block();
    }

    public void release(Long roomId, ReleaseRoomRequest request) {
        webClient.post()
                .uri(baseUrl + "/api/rooms/{id}/release", roomId)
                .header("X-Internal-Key", internalKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(retrySpec())
                .block();
    }

    private Retry retrySpec() {
        return Retry.backoff(retries, Duration.ofMillis(backoffMs))
                .filter(ex -> {
                    if (ex instanceof ResponseStatusException) {
                        return false;
                    }
                    if (ex instanceof WebClientResponseException responseException) {
                        return responseException.getStatusCode().is5xxServerError();
                    }
                    return true;
                })
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }
}
