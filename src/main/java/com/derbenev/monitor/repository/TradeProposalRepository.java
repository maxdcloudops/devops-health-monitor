package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.ProposalStatus;
import com.derbenev.monitor.model.TradeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeProposalRepository extends JpaRepository<TradeProposal, Long> {

    List<TradeProposal> findByBotIdOrderByCreatedAtDesc(Long botId);
    List<TradeProposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);
    boolean existsByBotIdAndSymbolAndStatus(Long botId, String symbol, ProposalStatus status);
}
