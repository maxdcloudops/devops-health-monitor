package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.Bot;
import com.derbenev.monitor.model.BotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {

    List<Bot> findByStatus(BotStatus status);
    List<Bot> findByNameContainingIgnoreCase(String name);

    @Query("SELECT b FROM Bot b WHERE b.status = :status "
            + "AND (b.lastHeartbeatAt IS NULL OR b.lastHeartbeatAt < :threshold)")
    List<Bot> findStale(@Param("status") BotStatus status, @Param("threshold") LocalDateTime threshold);
}
