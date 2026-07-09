# DevOps Health Monitor — Project Context

## Цель проекта
Веб-приложение для мониторинга облачных ресурсов (AWS, GCP) с визуализацией в виде графиков, диаграмм и схем архитектуры.

## Кто разрабатывает
- **Maks** — DevOps/CloudOps инженер (AWS), никогда не писал код
- **Claude** — учитель, объясняет каждый шаг
- Формат: пара учитель/ученик. Объяснять ЧТО делаем и ПОЧЕМУ, не просто давать код.

## Стек
- **Backend**: Java 26, Spring Boot 4.1.0, Maven
- **БД**: планируем H2 (для разработки) → PostgreSQL (для прода)
- **Frontend**: пока не выбран (Thymeleaf / React / встроенный в Spring)
- **Облака**: AWS (основной), GCP (планируется)
- **Мониторинг**: AWS SDK для Java, возможно GCP SDK

## Что хотим видеть в UI
- Графики (CPU, memory, network по инстансам)
- Диаграммы (схемы архитектуры — что где запущено)
- Дашборд по ресурсам (EC2, RDS, S3, Lambda и т.д.)

## Текущее состояние кода

### Написано (но с багами):
- `DevopsHealthMonitorApplication.java` — точка входа ✅
- `model/Endpoint.java` — JPA entity ⚠️ баги (см. ниже)
- `repository/EndpointRepository.java` — репозиторий ⚠️ баги

### Баги которые надо исправить:
1. `Endpoint.java`: `Generationtype` → `GenerationType` (регистр!)
2. `Endpoint.java`: пропущена `;` после `private String lastStatus`
3. `EndpointRepository.java`: `findByActivetrue()` → `findByActiveTrue()`
4. `pom.xml`: используется `spring-boot-starter-data-jdbc`, но код написан под JPA → нужно заменить на `spring-boot-starter-data-jpa`

### Не написано:
- `service/EndpointService.java` ❌
- `controller/EndpointController.java` ❌
- Настройка БД в `application.properties` ❌
- Интеграция с AWS SDK ❌
- Frontend/UI ❌

## План разработки (по шагам)

### Фаза 1 — Основа (делаем сейчас)
1. Исправить баги, добавить JPA в pom.xml
2. Настроить H2 БД
3. Написать Service слой
4. Написать Controller (REST API)
5. Запустить и проверить

### Фаза 2 — Модель данных для облака
- Переименовать/расширить `Endpoint` → добавить CloudResource entity
- EC2, RDS, S3, Lambda как типы ресурсов
- Регион, аккаунт, теги

### Фаза 3 — AWS интеграция
- AWS SDK v2 для Java
- Получение списка EC2 инстансов
- CloudWatch метрики (CPU, memory)
- Периодическая синхронизация (@Scheduled)

### Фаза 4 — Frontend с графиками
- Выбор: Thymeleaf + Chart.js (проще) или React (сложнее но мощнее)
- Дашборд с графиками
- Схема архитектуры

### Фаза 5 — Прод готовность
- PostgreSQL вместо H2
- Docker + docker-compose
- GCP интеграция

## Ключевые концепции Java для изучения
(в порядке прохождения)
- [ ] Аннотации (@Entity, @RestController, @Service...)
- [ ] Spring слои: Controller → Service → Repository → DB
- [ ] JPA / Hibernate (маппинг объектов в таблицы)
- [ ] REST API (GET, POST, PUT, DELETE)
- [ ] @Scheduled (периодические задачи)
- [ ] Dependency Injection (@Autowired, конструктор)
- [ ] DTO паттерн
- [ ] Exception handling
- [ ] Тесты (JUnit 5, Mockito)
- [ ] Docker для Java приложений
