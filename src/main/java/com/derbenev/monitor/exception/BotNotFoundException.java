package com.derbenev.monitor.exception;

public class BotNotFoundException extends RuntimeException {

    public BotNotFoundException(Long id) {
        super("Bot not found: id=" + id);
    }
}
