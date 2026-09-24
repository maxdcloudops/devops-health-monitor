package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.service.BotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final BotService botService;

    public DashboardController(BotService botService) {
        this.botService = botService;
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
}
