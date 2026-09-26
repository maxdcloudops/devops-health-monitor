package com.derbenev.monitor.service;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.DecisionLog;
import com.derbenev.monitor.repository.DecisionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DecisionLogService {

    private final DecisionLogRepository decisionLogRepository;
    private final BotService botService;

    public DecisionLogService(DecisionLogRepository decisionLogRepository, BotService botService) {
        this.decisionLogRepository = decisionLogRepository;
        this.botService = botService;
    }

    public List<DecisionLog> listByBot(Long botId) {
        botService.getById(botId);
        return decisionLogRepository.findByBotIdOrderByTimestampDesc(botId);
    }

    public DecisionLog record(Long botId, DecisionLog decisionLog) {
        Bot bot = botService.getById(botId);
        decisionLog.setId(null);
        decisionLog.setBot(bot);
        return decisionLogRepository.save(decisionLog);
    }
}
