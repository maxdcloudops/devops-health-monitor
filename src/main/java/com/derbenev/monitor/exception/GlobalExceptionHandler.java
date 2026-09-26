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
}
