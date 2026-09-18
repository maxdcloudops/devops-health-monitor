package com.derbenev.monitor.exception;

public class EndpointNotFoundException extends RuntimeException {

    public EndpointNotFoundException(Long id) {
        super("Endpoint not found: id=" + id);
    }
}
