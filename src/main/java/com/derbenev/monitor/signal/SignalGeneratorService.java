package com.derbenev.monitor.signal;

import com.derbenev.monitor.model.ProposalStatus;
import com.derbenev.monitor.model.TradeProposal;
import com.derbenev.monitor.model.TradeSide;
import com.derbenev.monitor.repository.TradeProposalRepository;
import com.derbenev.monitor.service.TradeProposalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Реальный источник сигналов: периодически считает MACD по настоящим дневным ценам и, если
 * видит свежее пересечение, создаёт {@link TradeProposal} - но НИКОГДА не вызывает approve/reject
 * сам. Это сознательная граница: агент предлагает, человек решает (см. PROJECT_CONTEXT.md,
 * Фаза 6). По умолчанию отключён (`app.signal.enabled=false`), чтобы не плодить сетевые вызовы
 * и предложения без явного решения владельца инструмента.
 */
@Service
public class SignalGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SignalGeneratorService.class);

    private final MarketDataClient marketDataClient;
    private final TradeProposalService tradeProposalService;
    private final TradeProposalRepository tradeProposalRepository;
    private final boolean enabled;
    private final Long botId;
    private final String symbol;
    private final BigDecimal quantity;

    public SignalGeneratorService(
            MarketDataClient marketDataClient,
            TradeProposalService tradeProposalService,
            TradeProposalRepository tradeProposalRepository,
            @Value("${app.signal.enabled:false}") boolean enabled,
            @Value("${app.signal.bot-id:0}") Long botId,
            @Value("${app.signal.symbol:AAPL}") String symbol,
            @Value("${app.signal.quantity:1}") BigDecimal quantity) {
        this.marketDataClient = marketDataClient;
        this.tradeProposalService = tradeProposalService;
        this.tradeProposalRepository = tradeProposalRepository;
        this.enabled = enabled;
        this.botId = botId;
        this.symbol = symbol;
        this.quantity = quantity;
    }

    @Scheduled(fixedDelayString = "${app.signal.check-interval-ms:3600000}")
    public void checkForSignal() {
        if (!enabled) {
            return;
        }
        try {
            List<Double> closes = marketDataClient.fetchDailyCloses(symbol);
            MacdCalculator.detectCrossover(closes)
                    .ifPresent(signal -> propose(signal, closes.get(closes.size() - 1)));
        } catch (Exception e) {
            // Сбой получения рыночных данных не должен ронять приложение - просто пробуем
            // на следующем тике по расписанию.
            log.warn("Проверка сигнала по {} не удалась: {}", symbol, e.getMessage());
        }
    }

    private void propose(MacdCalculator.CrossoverSignal signal, double lastClosePrice) {
        if (tradeProposalRepository.existsByBotIdAndSymbolAndStatus(botId, symbol, ProposalStatus.PENDING)) {
            log.info("Пропускаю новый сигнал {} по {}: уже есть неразобранное предложение", signal.side(), symbol);
            return;
        }

        TradeProposal proposal = new TradeProposal();
        proposal.setSymbol(symbol);
        proposal.setSide(signal.side());
        proposal.setQuantity(quantity);
        proposal.setPrice(BigDecimal.valueOf(lastClosePrice));
        proposal.setReasoning(String.format(Locale.ROOT,
                "MACD-сигнал по дневным свечам %s: MACD=%.4f, сигнальная линия=%.4f (%s). "
                        + "Источник данных: Yahoo Finance (дневные цены закрытия).",
                symbol, signal.macd(), signal.signal(),
                signal.side() == TradeSide.BUY ? "пересечение вверх" : "пересечение вниз"));

        tradeProposalService.propose(botId, proposal);
        log.info("Создано предложение {} по {} для бота {}", signal.side(), symbol, botId);
    }
}
