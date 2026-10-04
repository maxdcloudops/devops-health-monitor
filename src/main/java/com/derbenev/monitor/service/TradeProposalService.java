package com.derbenev.monitor.service;

import com.derbenev.monitor.exception.ProposalAlreadyDecidedException;
import com.derbenev.monitor.exception.TradeProposalNotFoundException;
import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.DecisionLog;
import com.derbenev.monitor.model.ProposalStatus;
import com.derbenev.monitor.model.Trade;
import com.derbenev.monitor.model.TradeProposal;
import com.derbenev.monitor.notification.TelegramProposalNotifier;
import com.derbenev.monitor.repository.TradeProposalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Агент-советник предлагает сделку и объясняет почему, но ничего не исполняет сам -
 * сделка становится настоящим {@link Trade} только после того, как человек явно
 * подтвердил предложение (approve). Это сознательное архитектурное решение: деньгами
 * и ключами брокера распоряжается человек, а не агент.
 */
@Service
public class TradeProposalService {

    private final TradeProposalRepository tradeProposalRepository;
    private final BotService botService;
    private final TradeService tradeService;
    private final DecisionLogService decisionLogService;
    private final TelegramProposalNotifier proposalNotifier;

    public TradeProposalService(
            TradeProposalRepository tradeProposalRepository,
            BotService botService,
            TradeService tradeService,
            DecisionLogService decisionLogService,
            TelegramProposalNotifier proposalNotifier) {
        this.tradeProposalRepository = tradeProposalRepository;
        this.botService = botService;
        this.tradeService = tradeService;
        this.decisionLogService = decisionLogService;
        this.proposalNotifier = proposalNotifier;
    }

    public List<TradeProposal> listByBot(Long botId) {
        botService.getById(botId);
        return tradeProposalRepository.findByBotIdOrderByCreatedAtDesc(botId);
    }

    public List<TradeProposal> listPending() {
        return tradeProposalRepository.findByStatusOrderByCreatedAtDesc(ProposalStatus.PENDING);
    }

    public TradeProposal propose(Long botId, TradeProposal proposal) {
        Bot bot = botService.getById(botId);
        proposal.setId(null);
        proposal.setBot(bot);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setCreatedAt(LocalDateTime.now());
        proposal.setDecidedAt(null);
        TradeProposal saved = tradeProposalRepository.save(proposal);
        proposalNotifier.notifyNewProposal(saved);
        return saved;
    }

    public Trade approve(Long proposalId) {
        TradeProposal proposal = getPendingOrThrow(proposalId);

        Trade trade = new Trade();
        trade.setSymbol(proposal.getSymbol());
        trade.setSide(proposal.getSide());
        trade.setQuantity(proposal.getQuantity());
        trade.setPrice(proposal.getPrice());
        trade.setExecutedAt(LocalDateTime.now());
        Trade savedTrade = tradeService.record(proposal.getBot().getId(), trade);

        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setDecidedAt(LocalDateTime.now());
        tradeProposalRepository.save(proposal);

        logDecision(proposal, "APPROVED " + proposal.getSide() + " " + proposal.getSymbol()
                + " " + proposal.getQuantity());

        return savedTrade;
    }

    public TradeProposal reject(Long proposalId) {
        TradeProposal proposal = getPendingOrThrow(proposalId);

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setDecidedAt(LocalDateTime.now());
        TradeProposal saved = tradeProposalRepository.save(proposal);

        logDecision(proposal, "REJECTED " + proposal.getSide() + " " + proposal.getSymbol()
                + " " + proposal.getQuantity());

        return saved;
    }

    private TradeProposal getPendingOrThrow(Long proposalId) {
        TradeProposal proposal = tradeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new TradeProposalNotFoundException(proposalId));
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ProposalAlreadyDecidedException(proposalId, proposal.getStatus());
        }
        return proposal;
    }

    private void logDecision(TradeProposal proposal, String action) {
        DecisionLog decisionLog = new DecisionLog();
        decisionLog.setAction(action);
        decisionLog.setReasoning(proposal.getReasoning());
        decisionLog.setTimestamp(LocalDateTime.now());
        decisionLogService.record(proposal.getBot().getId(), decisionLog);
    }
}
