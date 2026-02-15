# Hotel Booking Microservices (Spring Boot)

Многомодульный проект с 4 сервисами:

- `eureka-server` (8761)
- `api-gateway` (8080)
- `booking-service` (8081)
- `hotel-service` (8082)

## Что реализовано

- Service Discovery через Eureka.
- API Gateway c маршрутизацией:
  - `/api/bookings/**`, `/api/booking/**`, `/api/user/**` -> `booking-service`
  - `/api/hotels/**`, `/api/rooms/**` -> `hotel-service`
- JWT-аутентификация в `booking-service` и `hotel-service` (Resource Server, HMAC).
- Роли `USER` / `ADMIN`.
- `booking-service`:
  - регистрация/авторизация;
  - CRUD пользователей для ADMIN;
  - бронирования с состояниями `PENDING -> CONFIRMED` / `PENDING -> CANCELLED`;
  - идемпотентность по `requestId`;
  - retry/backoff/timeout при вызове `hotel-service`;
  - компенсация через `POST /api/rooms/{id}/release`.
- `hotel-service`:
  - CRUD отелей/комнат;
  - выборка свободных комнат на даты;
  - рекомендованные комнаты (сортировка по `timesBooked`, затем `id`);
  - internal endpoints для confirm/release с `X-Internal-Key`;
  - защита от гонок через pessimistic lock + проверку пересечений.
- H2 in-memory в каждом сервисе.
- Swagger/OpenAPI (`/swagger-ui.html`) в `booking-service` и `hotel-service`.
- Тесты JUnit: сценарии успеха/ошибки/тайм-аута/повторной доставки + MockMvc smoke test.

## Запуск

1. Запустить `eureka-server`.
2. Запустить `hotel-service`.
3. Запустить `booking-service`.
4. Запустить `api-gateway`.

Общий секрет JWT и internal key:

- `JWT_SECRET` (длина >= 32)
- `INTERNAL_SERVICE_KEY`

## Основные endpoint'ы

- Booking:
  - `POST /api/user/register`
  - `POST /api/user/auth`
  - `POST /api/user` (ADMIN)
  - `PATCH /api/user` (ADMIN)
  - `DELETE /api/user?id={id}` (ADMIN)
  - `POST /api/booking` (USER/ADMIN)
  - `GET /api/bookings` (USER/ADMIN)
  - `GET /api/booking/{id}` (USER/ADMIN)
  - `DELETE /api/booking/{id}` (USER/ADMIN)

- Hotel:
  - `POST /api/hotels` (ADMIN)
  - `POST /api/rooms` (ADMIN)
  - `GET /api/hotels` (USER/ADMIN)
  - `GET /api/rooms?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` (USER/ADMIN)
  - `GET /api/rooms/recommend?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` (USER/ADMIN)
  - `POST /api/rooms/{id}/confirm-availability` (internal)
  - `POST /api/rooms/{id}/release` (internal)
