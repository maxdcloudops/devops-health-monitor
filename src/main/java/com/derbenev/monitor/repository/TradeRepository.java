package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByBotIdOrderByExecutedAtDesc(Long botId);

    @Query("SELECT COALESCE(SUM(t.pnl), 0) FROM Trade t WHERE t.bot.id = :botId AND t.pnl IS NOT NULL")
    BigDecimal sumPnlByBotId(@Param("botId") Long botId);
}
