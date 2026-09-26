package com.derbenev.monitor.service;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.Trade;
import com.derbenev.monitor.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final BotService botService;

    public TradeService(TradeRepository tradeRepository, BotService botService) {
        this.tradeRepository = tradeRepository;
        this.botService = botService;
    }

    public List<Trade> listByBot(Long botId) {
        botService.getById(botId);
        return tradeRepository.findByBotIdOrderByExecutedAtDesc(botId);
    }

    public Trade record(Long botId, Trade trade) {
        Bot bot = botService.getById(botId);
        trade.setId(null);
        trade.setBot(bot);
        return tradeRepository.save(trade);
    }

    public BigDecimal totalPnl(Long botId) {
        return tradeRepository.sumPnlByBotId(botId);
    }
}
