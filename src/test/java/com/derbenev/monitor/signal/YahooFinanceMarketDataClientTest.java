package com.derbenev.monitor.signal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Живой сетевой вызов к Yahoo Finance не проверить из этой песочницы - организационная
 * политика блокирует финансовые/биржевые API-хосты на уровне прокси (stooq.com,
 * query1.finance.yahoo.com, api.coingecko.com, api.binance.com - все вернули 403 policy
 * denial). Поэтому разбор JSON проверяется здесь на заготовленном примере в точной форме
 * настоящего ответа Yahoo Finance - сама HTTP-часть ({@link YahooFinanceMarketDataClient#fetchDailyCloses})
 * на реальной машине с обычным интернетом (без такого прокси) должна работать без изменений,
 * но лично "кликнуть и увидеть" этот конкретный вызов я не смог.
 */
class YahooFinanceMarketDataClientTest {

    private static final String SAMPLE_RESPONSE = """
            {
              "chart": {
                "result": [
                  {
                    "timestamp": [1690000000, 1690086400, 1690172800, 1690259200, 1690345600],
                    "indicators": {
                      "quote": [
                        {
                          "close": [190.1, null, 191.25, 189.98, 192.44]
                        }
                      ]
                    }
                  }
                ],
                "error": null
              }
            }
            """;

    private static final String ERROR_RESPONSE = """
            {
              "chart": {
                "result": null,
                "error": {"code": "Not Found", "description": "No data found, symbol may be delisted"}
              }
            }
            """;

    @Test
    void parseClosesSkipsNullDaysAndKeepsOrder() {
        YahooFinanceMarketDataClient client = new YahooFinanceMarketDataClient();

        List<Double> closes = client.parseCloses(SAMPLE_RESPONSE, "AAPL");

        assertEquals(List.of(190.1, 191.25, 189.98, 192.44), closes);
    }

    @Test
    void parseClosesThrowsMarketDataExceptionWhenResultIsMissing() {
        YahooFinanceMarketDataClient client = new YahooFinanceMarketDataClient();

        assertThrows(MarketDataException.class, () -> client.parseCloses(ERROR_RESPONSE, "BADSYMBOL"));
    }

    @Test
    void parseClosesThrowsMarketDataExceptionOnInvalidJson() {
        YahooFinanceMarketDataClient client = new YahooFinanceMarketDataClient();

        assertThrows(MarketDataException.class, () -> client.parseCloses("not json at all", "AAPL"));
    }
}
