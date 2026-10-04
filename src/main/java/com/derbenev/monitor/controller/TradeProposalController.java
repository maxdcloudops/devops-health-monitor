package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Trade;
import com.derbenev.monitor.model.TradeProposal;
import com.derbenev.monitor.service.TradeProposalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API для агента-советника: сюда он присылает предложение сделки с объяснением
 * (POST .../proposals), а человек подтверждает или отклоняет его сам - через этот же API
 * или через дашборд. Агент никогда не вызывает approve/reject сам.
 */
@RestController
public class TradeProposalController {

    private final TradeProposalService tradeProposalService;

    public TradeProposalController(TradeProposalService tradeProposalService) {
        this.tradeProposalService = tradeProposalService;
    }

    @GetMapping("/api/bots/{botId}/proposals")
    public List<TradeProposal> list(@PathVariable Long botId) {
        return tradeProposalService.listByBot(botId);
    }

    @GetMapping("/api/proposals/pending")
    public List<TradeProposal> listPending() {
        return tradeProposalService.listPending();
    }

    @PostMapping("/api/bots/{botId}/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public TradeProposal propose(@PathVariable Long botId, @RequestBody TradeProposal proposal) {
        return tradeProposalService.propose(botId, proposal);
    }

    @PostMapping("/api/proposals/{id}/approve")
    public Trade approve(@PathVariable Long id) {
        return tradeProposalService.approve(id);
    }

    @PostMapping("/api/proposals/{id}/reject")
    public TradeProposal reject(@PathVariable Long id) {
        return tradeProposalService.reject(id);
    }
}
