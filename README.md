# High-Load Seat Booking Service

Учебный проект для статьи на Хабр: путь от наивной реализации бронирования
мест до production-grade решения, с честными графиками нагрузки на каждом шаге.

**Домен:** бронирование мест на мероприятие. Естественно порождает race
condition — нельзя продать одно и то же место дважды.

## Текущий этап: Stage 2 (pessimistic locking)

Добавлено:
- `PessimisticLockBookingService` — `SELECT ... FOR UPDATE` через
  `SeatRepository.findByIdForUpdate` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
  Лок берётся уже на чтении, а не только неявно на записи, как у Postgres по
  умолчанию — это и закрывает окно гонки, которое оставляла naive-реализация.
- Тест переструктурирован: логика гонки вынесена в абстрактный
  `BookingRaceConditionTestBase`, конкретные классы (`NaiveBookingRaceConditionTest`,
  `PessimisticLockRaceConditionTest`) — это просто `@ActiveProfiles` + наследование.
  Один и тот же тест: красный на naive, зелёный на pessimistic — без единой
  скопированной строчки.

### Как переключиться и прогнать

Меняешь профиль в `application.yml`:
```yaml
spring:
  profiles:
    active: pessimistic   # было naive
```

```bash
docker compose down -v && docker compose up -d
mvn spring-boot:run
```

Тест (в другом терминале, приложение может даже не быть запущено — Testcontainers поднимет свою БД):
```bash
mvn test -Dtest=PessimisticLockRaceConditionTest
```
**Ожидаемый результат: тест проходит.** Ровно та же проверка, что валила naive-версию.

Нагрузочный тест — теми же параметрами, что и на Stage 1, для честного сравнения:
```bash
mvn clean gatling:test -Dusers=200 -Dduration=30
```

Ещё не реализовано (следующие этапы):
- `OptimisticLockBookingService` — `@Version` + retry
- `RedisLockBookingService` — распределённый лок через Redisson
- Kafka-based partitioned booking
- Observability: Micrometer + Prometheus + Grafana дашборд

## Как запустить

### 1. Поднять Postgres

```bash
docker compose up -d
```

### 2. Собрать и запустить приложение

```bash
mvn spring-boot:run
```

Приложение поднимется на `:8080`, активный профиль — `naive` (задаётся в
`application.yml`, ключ `spring.profiles.active`). При старте `DataSeeder`
создаёт одно событие и 200 мест (в т.ч. "R1-S1" — так называемое hot seat
для нагрузочных тестов).

### 3. Проверить руками

```bash
curl -X POST localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"seatId": 1, "userId": "alice"}'
```

### 4. Прогнать тест, который ловит баг

```bash
mvn test -Dtest=NaiveBookingRaceConditionTest
```

**Ожидаемый результат на этом этапе: тест падает.** Смотри вывод в консоли —
там honest-репорт: сколько HTTP 201 реально пришло и сколько строк
`Booking` реально оказалось в базе для одного места. Обычно это число
больше единицы при 50 параллельных запросах — вот он, double-booking.

Тест поднимает свой собственный Postgres через Testcontainers, так что для
него `docker compose up` не обязателен (но Docker демон должен быть
доступен).

### 5. Погонять нагрузочный тест

```bash
mvn gatling:test -Dusers=200 -Dduration=30
```

Результаты — в `target/gatling-results/.../index.html`. Держи графики с
каждого этапа (naive / pessimistic / optimistic / redis) — это и есть
материал для сравнительных графиков в статье.

## Структура

```
src/main/java/dev/booking/
├── domain/           Event, Seat, Booking, SeatStatus
├── repository/       Spring Data JPA репозитории
├── service/          BookingService (стратегия) + NaiveBookingService
├── controller/       REST-эндпоинты
├── dto/               request/response records
├── exception/        доменные исключения + @RestControllerAdvice
└── config/           DataSeeder (сидинг тестовых данных)

src/test/java/dev/booking/concurrency/
└── NaiveBookingRaceConditionTest.java   ← тест, ловящий гонку

src/gatling/java/dev/booking/simulations/
└── BookingLoadSimulation.java           ← нагрузочные сценарии
```

## Почему `version` в Seat есть, но не работает как @Version

Колонка добавлена заранее, чтобы на этапе optimistic locking не пришлось
делать миграцию схемы — переход будет чисто в коде (`@Version` над тем же
полем). Это осознанное решение, а не забытый код.
