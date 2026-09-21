package com.derbenev.monitor.service;

import com.derbenev.monitor.exception.BotNotFoundException;
import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.BotStatus;
import com.derbenev.monitor.repository.BotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BotService {

    private final BotRepository botRepository;

    public BotService(BotRepository botRepository) {
        this.botRepository = botRepository;
    }

    public List<Bot> getAll() {
        return botRepository.findAll();
    }

    public List<Bot> getByStatus(BotStatus status) {
        return botRepository.findByStatus(status);
    }

    public List<Bot> searchByName(String name) {
        return botRepository.findByNameContainingIgnoreCase(name);
    }

    public Bot getById(Long id) {
        return botRepository.findById(id)
                .orElseThrow(() -> new BotNotFoundException(id));
    }

    public Bot create(Bot bot) {
        bot.setId(null);
        bot.setCreatedAt(LocalDateTime.now());
        if (bot.getStatus() == null) {
            bot.setStatus(BotStatus.STOPPED);
        }
        return botRepository.save(bot);
    }

    public Bot update(Long id, Bot update) {
        Bot existing = getById(id);
        existing.setName(update.getName());
        existing.setBroker(update.getBroker());
        existing.setStrategy(update.getStrategy());
        existing.setStatus(update.getStatus());
        return botRepository.save(existing);
    }

    public Bot recordHeartbeat(Long id) {
        Bot bot = getById(id);
        bot.setLastHeartbeatAt(LocalDateTime.now());
        bot.setStatus(BotStatus.RUNNING);
        return botRepository.save(bot);
    }

    public void delete(Long id) {
        if (!botRepository.existsById(id)) {
            throw new BotNotFoundException(id);
        }
        botRepository.deleteById(id);
    }
}
