package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.DecisionLog;
import com.derbenev.monitor.service.DecisionLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots/{botId}/decisions")
public class DecisionLogController {

    private final DecisionLogService decisionLogService;

    public DecisionLogController(DecisionLogService decisionLogService) {
        this.decisionLogService = decisionLogService;
    }

    @GetMapping
    public List<DecisionLog> list(@PathVariable Long botId) {
        return decisionLogService.listByBot(botId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionLog record(@PathVariable Long botId, @RequestBody DecisionLog decisionLog) {
        return decisionLogService.record(botId, decisionLog);
    }
}
