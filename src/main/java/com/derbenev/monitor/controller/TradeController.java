package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Trade;
import com.derbenev.monitor.service.TradeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots/{botId}/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public List<Trade> list(@PathVariable Long botId) {
        return tradeService.listByBot(botId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Trade record(@PathVariable Long botId, @RequestBody Trade trade) {
        return tradeService.record(botId, trade);
    }
}
