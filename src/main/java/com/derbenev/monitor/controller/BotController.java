package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.BotStatus;
import com.derbenev.monitor.service.BotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
public class BotController {

    private final BotService botService;

    public BotController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping
    public List<Bot> getAll(
            @RequestParam(required = false) BotStatus status,
            @RequestParam(required = false) String name) {
        if (name != null) {
            return botService.searchByName(name);
        }
        if (status != null) {
            return botService.getByStatus(status);
        }
        return botService.getAll();
    }

    @GetMapping("/{id}")
    public Bot getById(@PathVariable Long id) {
        return botService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Bot create(@RequestBody Bot bot) {
        return botService.create(bot);
    }

    @PutMapping("/{id}")
    public Bot update(@PathVariable Long id, @RequestBody Bot bot) {
        return botService.update(id, bot);
    }

    @PostMapping("/{id}/heartbeat")
    public Bot recordHeartbeat(@PathVariable Long id) {
        return botService.recordHeartbeat(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        botService.delete(id);
    }
}
