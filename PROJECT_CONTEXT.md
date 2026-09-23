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
- `BotService.markStaleBotsDown()` — `@Scheduled` задача (интервал из
  `app.bot.heartbeat-check-interval-ms`, по умолчанию 60 сек): находит ботов со статусом
  RUNNING, у которых `lastHeartbeatAt` старше `app.bot.heartbeat-timeout-minutes` (по умолчанию
  5 мин) или вообще `null`, переводит их в DOWN и пишет `WARN` в лог с именем бота и временем
  последнего heartbeat ✅
- `@EnableScheduling` на `DevopsHealthMonitorApplication` — включает работу `@Scheduled` ✅
- `application.properties` — H2 подключена и работает ✅

### Решено: `java.version` в `pom.xml` понижен с 26 до 21 ✅
Контейнер разработки даёт только JDK 21, Java 26 ещё не вышла как релиз. `./mvnw test`
теперь проходит без ручных флагов.

### Решено: Spring Security убран из `pom.xml` ✅
`spring-boot-starter-security` и `spring-boot-starter-security-oauth2-resource-server` (плюс
их `-test` артефакты) удалены — они были подключены без единой настройки (`issuer-uri`,
`SecurityFilterChain`, пользователь/пароль) и блокировали весь API 401-м без возможности
разобраться руками. Решили вернуть их в Фазе 5 вместе с прод-готовностью, когда будет ясно,
какая аутентификация нужна (Basic/JWT/OAuth2). До тех пор API открыт — это осознанный выбор
для локальной разработки, не для прода.

### Найден и исправлен баг: `Bot.status` был `null` при создании через API 🐛✅
При `POST /api/bots` без поля `status` в теле запроса Hibernate падал с
`DataIntegrityViolationException: not-null property references a null ... Bot.status`.
Причина: поле `status = BotStatus.STOPPED` в `Bot.java` — это Java-инициализатор поля, он
срабатывает только при вызове конструктора; а Jackson при десериализации JSON в `Bot` в этом
случае использовал не no-args-конструктор + сеттеры (что дало бы `status = STOPPED` от
инициализатора), а другой путь создания объекта, при котором инициализатор не применяется, и
поле осталось `null`. Проверил `new Bot()` напрямую в маленьком тестовом классе — там `status`
действительно равен `STOPPED`, значит дело именно в пути десериализации, а не в самом
инициализаторе. **Урок**: нельзя полагаться на дефолт-значение поля-инициализатора для данных,
которые приходят через Jackson/JPA — сервис должен сам гарантировать дефолт. Фикс —
`BotService.create()` теперь явно ставит `STOPPED`, если `status` не пришёл:
```java
if (bot.getStatus() == null) {
    bot.setStatus(BotStatus.STOPPED);
}
```

### Проверено end-to-end вручную через curl ✅
Полный сценарий отработал без ошибок: создать бота (`POST /api/bots`, статус сразу `STOPPED`)
→ `POST /{id}/heartbeat` (статус переходит в `RUNNING`) → записать сделку
(`POST /{id}/trades`) → записать лог решения (`POST /{id}/decisions`) → получить список
сделок бота → `404 Bot not found` на несуществующий id.

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
- ~~Проверено через `./mvnw test` и вручную через curl~~ ✅ — полный сценарий (создать бота →
  heartbeat → записать сделку → записать решение → список сделок → 404 на несуществующий id)
  отработал без ошибок. По пути убрали недонастроенный Spring Security (блокировал весь API)
  и починили баг с `null` статусом бота при создании (см. разделы выше).

### Фаза 3 — Данные о ботах ✅ завершена
- ~~Ручной ввод сделок через REST API~~ ✅ (`POST /api/bots/{id}/trades`), проверено ещё в Фазе 2
- ~~`@Scheduled` heartbeat-проверка: бот молчит N минут → статус меняется на DOWN~~ ✅
  — проверено вживую: создал бота, отправил heartbeat (RUNNING), подождал больше
  `heartbeat-timeout-minutes` — бот сам перешёл в DOWN, в лог упало `WARN Bot ... marked DOWN`
- Позже: коннектор к paper-trading / Robinhood MCP для автоматического импорта сделок

Осталась учебная тема, не покрытая кодом явно, но уже видна на практике: `@Scheduled` теперь
из чек-листа ниже можно отметить пройденным — разобрали `fixedDelayString`, настройку интервала
через `application.properties` и то, почему heartbeat-проверка не трогает ботов в статусе
STOPPED (это намеренная остановка, а не сбой).

### Фаза 4 — Frontend с графиками
- Выбор: Thymeleaf + Chart.js (проще) или React (сложнее но мощнее)
- Дашборд: статус ботов, P&L график, лог решений с объяснениями
- Алерты (просадка выше порога, бот не отвечает)

### Фаза 5 — Прод готовность
- PostgreSQL вместо H2
- Docker + docker-compose
- Вернуть Spring Security (`spring-boot-starter-security` + выбрать между Basic/JWT/OAuth2),
  убранный в Фазе 2 как недонастроенный — см. раздел выше
- Опрос v1: показать MVP в r/ai_trading, собрать обратную связь по фичам

## Ключевые концепции Java для изучения
(в порядке прохождения)
- [ ] Аннотации (@Entity, @RestController, @Service...)
- [ ] Spring слои: Controller → Service → Repository → DB
- [ ] JPA / Hibernate (маппинг объектов в таблицы)
- [ ] REST API (GET, POST, PUT, DELETE)
- [x] @Scheduled (периодические задачи) — heartbeat-проверка ботов, Фаза 3
- [ ] Dependency Injection (@Autowired, конструктор)
- [ ] DTO паттерн
- [ ] Exception handling
- [ ] Тесты (JUnit 5, Mockito)
- [ ] Docker для Java приложений
