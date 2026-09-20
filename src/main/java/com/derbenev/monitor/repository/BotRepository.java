package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.BotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {

    List<Bot> findByStatus(BotStatus status);
    List<Bot> findByNameContainingIgnoreCase(String name);
}
