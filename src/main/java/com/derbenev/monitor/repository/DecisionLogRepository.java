package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.DecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionLogRepository extends JpaRepository<DecisionLog, Long> {

    List<DecisionLog> findByBotIdOrderByTimestampDesc(Long botId);
}
