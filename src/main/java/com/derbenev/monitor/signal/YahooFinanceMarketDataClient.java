package com.derbenev.monitor.signal;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Публичный, не требующий API-ключа JSON-эндпоинт Yahoo Finance - стандартный выбор для
 * дневных цен закрытия без регистрации брокерского аккаунта.
 */
@Component
public class YahooFinanceMarketDataClient implements MarketDataClient {

    private static final String CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=6mo";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YahooFinanceMarketDataClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public List<Double> fetchDailyCloses(String symbol) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(CHART_URL, symbol)))
                .header("User-Agent", "Mozilla/5.0 (compatible; BotOpsMonitor/1.0)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        String body;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new MarketDataException(
                        "Yahoo Finance вернул HTTP " + response.statusCode() + " для " + symbol);
            }
            body = response.body();
        } catch (IOException e) {
            throw new MarketDataException("Не удалось получить рыночные данные для " + symbol, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketDataException("Запрос рыночных данных для " + symbol + " прерван", e);
        }
        return parseCloses(body, symbol);
    }

    /** package-private - чтобы юнит-тест мог прогнать разбор на заранее заготовленном JSON без сети. */
    List<Double> parseCloses(String json, String symbol) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (RuntimeException e) {
            // Jackson 3 (tools.jackson) бросает непроверяемые исключения на разборе JSON -
            // не старый checked IOException.
            throw new MarketDataException("Не удалось разобрать ответ Yahoo Finance для " + symbol, e);
        }
        JsonNode result = root.path("chart").path("result").get(0);
        if (result == null) {
            throw new MarketDataException("Yahoo Finance не вернул данных для " + symbol);
        }
        JsonNode closesNode = result.path("indicators").path("quote").get(0).path("close");
        List<Double> closes = new ArrayList<>();
        for (JsonNode node : closesNode) {
            if (!node.isNull()) {
                closes.add(node.asDouble());
            }
        }
        return closes;
    }
}
