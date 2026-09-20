# Bot Ops Monitor — Project Context

## Разворот проекта (03.08.2026)
Изначально проект был мониторингом облачных ресурсов (AWS/GCP) — учебный пет-проект.
После разбора отчёта по Reddit-сигналам (r/ai_trading, r/algotrading, r/quant, 70 сигналов,
июнь-август 2026) решили сменить домен на прикладной продукт с реальным спросом,
сохранив тот же стек и тот же план обучения Java/Spring.

**Ключевой сигнал из отчёта**: аудитория AI-трейдинг-ботов (r/ai_trading, 51K подписчиков)
уже прошла стадию «хочу бота, который угадывает цену» и просит именно **операционный
слой вокруг агента** — то, в чём Maks как DevOps-инженер разбирается профессионально:
- мониторинг живости ботов, рестарты, изоляция (`r/ai_trading: "I run 13 paper trading
  bots on one EC2 instance under systemd"`)
- risk engine отдельно от стратегии
- объяснимость решений — самая частая жалоба не «бот не работает», а «не знаю, почему
  он принял это решение»
- лёгкая замена LLM-модели без переписывания логики
- P&L и статус по каждому боту в одном дашборде

Идея бота-предсказателя цены сознательно отклонена — отчёт полон примеров, что это
статистическая лотерея (виральные истории "100% wins" разбираются комьюнити за часы,
экс-квант BlackRock прямо предупреждает про переобучение агентов). Строим не трейдинг-бота,
а панель управления и мониторинга для чужих ботов.

## Цель проекта
Веб-приложение — панель мониторинга и управления торговыми ботами: живы ли боты, какие
позиции открыты, лог решений агента ("почему он это купил"), P&L по каждому боту,
алерты и возможность рестарта/остановки.

## Кто разрабатывает
- **Maks** — DevOps/CloudOps инженер (AWS), никогда не писал код
- **Claude** — учитель, объясняет каждый шаг
- Формат: пара учитель/ученик. Объяснять ЧТО делаем и ПОЧЕМУ, не просто давать код.

## Стек
- **Backend**: Java 26, Spring Boot 4.1.0, Maven
- **БД**: H2 (для разработки) → PostgreSQL (для прода)
- **Frontend**: пока не выбран (Thymeleaf / React / встроенный в Spring)
- **Источники данных о ботах**: сначала ручной/API ввод сделок, позже — коннекторы
  к брокерам (Robinhood MCP как де-факто стандарт в нише) и paper-trading окружениям

## Что хотим видеть в UI
- Статус каждого бота (жив/упал/когда последний heartbeat) — как у нас уже был `Endpoint`,
  только для торгового бота, а не облачного ресурса
- Лог решений бота (что купил/продал и почему — если агент это объясняет)
- Графики P&L, win rate, просадка по каждому боту
- Алерты (бот молчит N минут, просадка выше порога) и кнопка рестарта

## Текущее состояние кода

### Написано:
- `DevopsHealthMonitorApplication.java` — точка входа ✅
- `model/Bot.java` — id, name, broker, strategy, status (enum `BotStatus`: RUNNING/STOPPED/DOWN),
  createdAt, lastHeartbeatAt ✅
- `model/Trade.java` — сделка бота: symbol, side (enum `TradeSide`: BUY/SELL), quantity, price,
  pnl (все денежные поля — `BigDecimal`, не `double`, чтобы не терять точность), executedAt,
  связь `@ManyToOne` на `Bot` ✅
- `model/DecisionLog.java` — лог решений бота: action, reasoning (текст объяснения), timestamp,
  связь `@ManyToOne` на `Bot` ✅
- `repository/BotRepository.java`, `TradeRepository.java`, `DecisionLogRepository.java` ✅
- `service/BotService.java` — CRUD + `recordHeartbeat(id)` (обновляет lastHeartbeatAt и
  переводит статус в RUNNING) ✅
- `service/TradeService.java`, `DecisionLogService.java` — запись + список по боту; при записи
  бот всегда подставляется по id из URL на сервере (клиентский JSON с полем `bot` игнорируется,
  чтобы нельзя было привязать сделку к чужому боту) ✅
- `controller/BotController.java` — REST `/api/bots` (GET/POST/PUT/DELETE, фильтры по
  status/name, `POST /{id}/heartbeat`) ✅
- `controller/TradeController.java` — REST `/api/bots/{botId}/trades` (GET/POST) ✅
- `controller/DecisionLogController.java` — REST `/api/bots/{botId}/decisions` (GET/POST) ✅
- `exception/BotNotFoundException.java` + `exception/GlobalExceptionHandler.java`
  (`@RestControllerAdvice`, общий на все контроллеры) → 404 вместо 500 при несуществующем id ✅
- `application.properties` — H2 подключена и работает ✅

### Решено: `java.version` в `pom.xml` понижен с 26 до 21 ✅
Контейнер разработки даёт только JDK 21, Java 26 ещё не вышла как релиз. `./mvnw test`
теперь проходит без ручных флагов.

### Известная проблема окружения: Spring Security блокирует весь API (не решено) ⚠️
В `pom.xml` подключены `spring-boot-starter-security` и
`spring-boot-starter-security-oauth2-resource-server`, но нигде не настроен ни
`issuer-uri`, ни `SecurityFilterChain`, ни пользователь/пароль. В результате Spring Boot
по умолчанию требует HTTP Basic на все запросы (`401` на любой `/api/...`), а сгенерированный
пароль почему-то не печатается в лог при старте — разобраться в этом отдельно. Из-за этого
API проверялся только через Spring-контекст (`./mvnw test` — бины поднимаются, таблицы с FK
создаются), вручную через curl проверить не удалось: не помогло даже исключение
`SecurityAutoConfiguration` / `UserDetailsServiceAutoConfiguration` /
`OAuth2ResourceServerAutoConfiguration` через `SPRING_AUTOCONFIGURE_EXCLUDE`.
**Нужно решение**: либо убрать оба security-стартера, пока нет настоящей аутентификации
(вернуть их в Фазе 5 вместе с прод-готовностью), либо настроить security сейчас. Пока не
трогал `pom.xml` дальше — это решение, которое стоит обсудить с Maks, а не делать по-тихому.

### Не написано:
- Frontend/UI ❌

## План разработки (по шагам)

### Фаза 1 — Основа ✅ завершена (домен ещё старый — `Endpoint`)
1. ~~Исправить баги, добавить JPA в pom.xml~~ ✅
2. ~~Настроить H2 БД~~ ✅
3. ~~Написать Service слой~~ ✅
4. ~~Написать Controller (REST API)~~ ✅
5. ~~Запустить и проверить (CRUD по `Endpoint` работает end-to-end)~~ ✅ — `./mvnw test`
   зелёный, Spring-контекст поднимается, таблица `endpoints` создаётся в H2.

Эта фаза учебная — отработали слои Controller → Service → Repository на простой
модели, прежде чем переходить к более сложному домену ботов (Фаза 2).

### Фаза 2 — Разворот на домен ботов ✅ завершена
- ~~`Endpoint` → `Bot`~~ ✅
- ~~Новая entity `Trade`~~ ✅
- ~~Новая entity `DecisionLog`~~ ✅
- ~~Service/Controller под новые entity~~ ✅ (плюс вынесли обработку ошибок в общий
  `@RestControllerAdvice`, чтобы не дублировать `@ExceptionHandler` в трёх контроллерах)
- Проверено через `./mvnw test`: Spring-контекст поднимается, Hibernate создаёт таблицы
  `bots`/`trades`/`decision_logs` с внешними ключами на `bot_id`. Ручную проверку через curl
  заблокировал незавершённый Spring Security (см. раздел выше) — решить перед Фазой 3.

### Фаза 3 — Данные о ботах ← следующая
- Разобраться с Spring Security (см. известную проблему выше) — иначе руками API не проверить
- Ручной ввод сделок через REST API (уже есть — `POST /api/bots/{id}/trades`) — осталось
  проверить руками после решения security
- `@Scheduled` heartbeat-проверка: бот молчит N минут → статус меняется на DOWN
- Позже: коннектор к paper-trading / Robinhood MCP для автоматического импорта сделок

### Фаза 4 — Frontend с графиками
- Выбор: Thymeleaf + Chart.js (проще) или React (сложнее но мощнее)
- Дашборд: статус ботов, P&L график, лог решений с объяснениями
- Алерты (просадка выше порога, бот не отвечает)

### Фаза 5 — Прод готовность
- PostgreSQL вместо H2
- Docker + docker-compose
- Опрос v1: показать MVP в r/ai_trading, собрать обратную связь по фичам

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
