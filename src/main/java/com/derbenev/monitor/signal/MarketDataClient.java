package com.derbenev.monitor.signal;

import java.util.List;

/**
 * Источник настоящих рыночных цен. Интерфейс отделён от конкретного провайдера
 * ({@link YahooFinanceMarketDataClient}), чтобы провайдера можно было поменять без
 * изменения логики сигналов.
 */
public interface MarketDataClient {

    /**
     * Дневные цены закрытия по символу, от старых к новым, без пропусков (null-дни отфильтрованы).
     */
    List<Double> fetchDailyCloses(String symbol);
}
