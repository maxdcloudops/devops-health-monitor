package com.derbenev.monitor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BotNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleBotNotFound(BotNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(TradeProposalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleTradeProposalNotFound(TradeProposalNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ProposalAlreadyDecidedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleProposalAlreadyDecided(ProposalAlreadyDecidedException ex) {
        return ex.getMessage();
    }
}
