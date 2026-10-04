package com.derbenev.monitor.signal;

import com.derbenev.monitor.model.TradeSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Настоящая техническая индикаторная логика (EMA/MACD), не рандом и не мок - считает
 * тот же самый индикатор, который вручную подсчитывали комментаторы в разобранном Reddit-отчёте
 * ("the backtest, not the live week"). Разбита на маленькие чистые функции специально, чтобы
 * каждую можно было проверить юнит-тестом на вручную посчитанных числах - финансовую математику
 * нельзя "наверное работает", её нужно реально проверить.
 */
public final class MacdCalculator {

    private static final int FAST_PERIOD = 12;
    private static final int SLOW_PERIOD = 26;
    private static final int SIGNAL_PERIOD = 9;
    private static final int MIN_CLOSES_REQUIRED = SLOW_PERIOD + SIGNAL_PERIOD + 2;

    private MacdCalculator() {
    }

    public record CrossoverSignal(TradeSide side, double macd, double signal) {
    }

    /**
     * EMA по формуле EMA_t = price_t * k + EMA_(t-1) * (1-k), k = 2/(period+1),
     * первое значение - простая средняя (SMA) первых `period` точек.
     */
    public static List<Double> ema(List<Double> values, int period) {
        if (values.size() < period) {
            return List.of();
        }
        double k = 2.0 / (period + 1);
        List<Double> result = new ArrayList<>();
        double seed = 0;
        for (int i = 0; i < period; i++) {
            seed += values.get(i);
        }
        result.add(seed / period);
        for (int i = period; i < values.size(); i++) {
            double prev = result.get(result.size() - 1);
            result.add(values.get(i) * k + prev * (1 - k));
        }
        return result;
    }

    static List<Double> macdSeries(List<Double> closes) {
        List<Double> emaFast = ema(closes, FAST_PERIOD);
        List<Double> emaSlow = ema(closes, SLOW_PERIOD);
        int offset = emaFast.size() - emaSlow.size();
        List<Double> macd = new ArrayList<>();
        for (int i = 0; i < emaSlow.size(); i++) {
            macd.add(emaFast.get(i + offset) - emaSlow.get(i));
        }
        return macd;
    }

    /**
     * Пересечение MACD и сигнальной линии на последней паре точек (т.е. сигнал "только что
     * произошёл", а не "уже давно идёт в одну сторону").
     */
    public static Optional<CrossoverSignal> crossover(
            double macdPrev, double macdLast, double signalPrev, double signalLast) {
        if (macdPrev <= signalPrev && macdLast > signalLast) {
            return Optional.of(new CrossoverSignal(TradeSide.BUY, macdLast, signalLast));
        }
        if (macdPrev >= signalPrev && macdLast < signalLast) {
            return Optional.of(new CrossoverSignal(TradeSide.SELL, macdLast, signalLast));
        }
        return Optional.empty();
    }

    public static Optional<CrossoverSignal> detectCrossover(List<Double> closes) {
        if (closes.size() < MIN_CLOSES_REQUIRED) {
            return Optional.empty();
        }
        List<Double> macd = macdSeries(closes);
        List<Double> signal = ema(macd, SIGNAL_PERIOD);
        if (signal.size() < 2) {
            return Optional.empty();
        }
        double macdLast = macd.get(macd.size() - 1);
        double macdPrev = macd.get(macd.size() - 2);
        double signalLast = signal.get(signal.size() - 1);
        double signalPrev = signal.get(signal.size() - 2);
        return crossover(macdPrev, macdLast, signalPrev, signalLast);
    }
}
