package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.DecisionLog;
import com.derbenev.monitor.model.Trade;
import com.derbenev.monitor.model.TradeSide;
import com.derbenev.monitor.service.BotService;
import com.derbenev.monitor.service.DecisionLogService;
import com.derbenev.monitor.service.TradeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
public class DashboardController {

    private final BotService botService;
    private final TradeService tradeService;
    private final DecisionLogService decisionLogService;

    public DashboardController(
            BotService botService,
            TradeService tradeService,
            DecisionLogService decisionLogService) {
        this.botService = botService;
        this.tradeService = tradeService;
        this.decisionLogService = decisionLogService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("bots", botService.getAll());
        return "dashboard";
    }

    @GetMapping("/dashboard/bots/{id}")
    public String botDetail(@PathVariable Long id, Model model) {
        model.addAttribute("bot", botService.getById(id));
        return "bot-detail";
    }

    @PostMapping("/dashboard/bots")
    public String createBot(
            @RequestParam String name,
            @RequestParam String broker,
            @RequestParam String strategy) {
        Bot bot = new Bot();
        bot.setName(name);
        bot.setBroker(broker);
        bot.setStrategy(strategy);
        botService.create(bot);
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/bots/{id}/trades")
    public String createTrade(
            @PathVariable Long id,
            @RequestParam String symbol,
            @RequestParam TradeSide side,
            @RequestParam BigDecimal quantity,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String pnl,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime executedAt) {
        Trade trade = new Trade();
        trade.setSymbol(symbol);
        trade.setSide(side);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setPnl((pnl == null || pnl.isBlank()) ? null : new BigDecimal(pnl));
        trade.setExecutedAt(executedAt);
        tradeService.record(id, trade);
        return "redirect:/dashboard/bots/" + id;
    }

    @PostMapping("/dashboard/bots/{id}/decisions")
    public String createDecision(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String reasoning,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime timestamp) {
        DecisionLog decisionLog = new DecisionLog();
        decisionLog.setAction(action);
        decisionLog.setReasoning(reasoning);
        decisionLog.setTimestamp(timestamp);
        decisionLogService.record(id, decisionLog);
        return "redirect:/dashboard/bots/" + id;
    }
}
