package com.derbenev.monitor.signal;

import com.derbenev.monitor.model.TradeSide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacdCalculatorTest {

    private static final double EPSILON = 1e-9;

    @Test
    void emaSeedsWithSimpleMovingAverageThenAppliesSmoothing() {
        // Вручную посчитано: values=[1,2,3,4,5], period=3, k=2/(3+1)=0.5
        // seed = (1+2+3)/3 = 2.0
        // i=3 (value=4): 4*0.5 + 2.0*0.5 = 3.0
        // i=4 (value=5): 5*0.5 + 3.0*0.5 = 4.0
        List<Double> result = MacdCalculator.ema(List.of(1.0, 2.0, 3.0, 4.0, 5.0), 3);

        assertEquals(3, result.size());
        assertEquals(2.0, result.get(0), EPSILON);
        assertEquals(3.0, result.get(1), EPSILON);
        assertEquals(4.0, result.get(2), EPSILON);
    }

    @Test
    void emaReturnsEmptyWhenNotEnoughData() {
        assertTrue(MacdCalculator.ema(List.of(1.0, 2.0), 5).isEmpty());
    }

    @Test
    void crossoverDetectsMacdMovingAboveSignal() {
        Optional<MacdCalculator.CrossoverSignal> signal =
                MacdCalculator.crossover(-1.0, 1.0, 0.0, 0.0);

        assertTrue(signal.isPresent());
        assertEquals(TradeSide.BUY, signal.get().side());
        assertEquals(1.0, signal.get().macd(), EPSILON);
        assertEquals(0.0, signal.get().signal(), EPSILON);
    }

    @Test
    void crossoverDetectsMacdMovingBelowSignal() {
        Optional<MacdCalculator.CrossoverSignal> signal =
                MacdCalculator.crossover(1.0, -1.0, 0.0, 0.0);

        assertTrue(signal.isPresent());
        assertEquals(TradeSide.SELL, signal.get().side());
    }

    @Test
    void crossoverIsEmptyWhenMacdStaysOnSameSide() {
        // MACD было и осталось выше сигнальной линии - это не "свежее" пересечение
        assertTrue(MacdCalculator.crossover(1.0, 2.0, 0.0, 0.0).isEmpty());
    }

    @Test
    void detectCrossoverReturnsEmptyWithoutEnoughHistory() {
        List<Double> tooFewCloses = List.of(1.0, 2.0, 3.0);
        assertTrue(MacdCalculator.detectCrossover(tooFewCloses).isEmpty());
    }

    @Test
    void detectCrossoverIsEmptyForFlatPriceSeries() {
        // Цена не двигается -> MACD и сигнальная линия обе тождественно равны нулю на всей
        // истории, "свежего" пересечения в этом состоянии быть не может ни на какой паре точек.
        List<Double> flatCloses = java.util.Collections.nCopies(50, 100.0);

        assertTrue(MacdCalculator.detectCrossover(flatCloses).isEmpty());
    }

    @Test
    void detectCrossoverUsesMinimumRequiredHistory() {
        // Ровно на границе "достаточно/недостаточно" данных (26 + 9 + 2 - 1 = 36 точек)
        // пересечения ещё не считаем - это тоже "недостаточно истории", а не ошибка.
        List<Double> closes = java.util.Collections.nCopies(36, 100.0);

        assertTrue(MacdCalculator.detectCrossover(closes).isEmpty());
    }
}
