package com.derbenev.monitor.notification;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.TradeProposal;
import com.derbenev.monitor.model.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramProposalNotifierTest {

    @Test
    void formatMessageIncludesBotSymbolSideAndReasoning() {
        Bot bot = new Bot();
        bot.setName("Моя стратегия");

        TradeProposal proposal = new TradeProposal();
        proposal.setBot(bot);
        proposal.setSymbol("AAPL");
        proposal.setSide(TradeSide.BUY);
        proposal.setQuantity(BigDecimal.valueOf(1));
        proposal.setPrice(BigDecimal.valueOf(192.44));
        proposal.setReasoning("MACD-сигнал: пересечение вверх");

        String message = TelegramProposalNotifier.formatMessage(proposal);

        assertTrue(message.contains("Моя стратегия"));
        assertTrue(message.contains("AAPL"));
        assertTrue(message.contains("BUY"));
        assertTrue(message.contains("192.44"));
        assertTrue(message.contains("MACD-сигнал: пересечение вверх"));
    }

    @Test
    void formatMessageHandlesMissingBotGracefully() {
        TradeProposal proposal = new TradeProposal();
        proposal.setSymbol("AAPL");
        proposal.setSide(TradeSide.SELL);
        proposal.setQuantity(BigDecimal.valueOf(2));
        proposal.setPrice(BigDecimal.valueOf(100));
        proposal.setReasoning("тест");

        String message = TelegramProposalNotifier.formatMessage(proposal);

        assertTrue(message.contains("?"));
    }
}
