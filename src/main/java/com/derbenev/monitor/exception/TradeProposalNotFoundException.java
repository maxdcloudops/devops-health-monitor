package com.derbenev.monitor.exception;

public class TradeProposalNotFoundException extends RuntimeException {

    public TradeProposalNotFoundException(Long id) {
        super("Trade proposal not found: id=" + id);
    }
}
