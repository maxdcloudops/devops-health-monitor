package com.derbenev.monitor.controller;

import com.derbenev.monitor.model.Endpoint;
import com.derbenev.monitor.service.EndpointService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endpoints")
public class EndpointController {

    private final EndpointService endpointService;

    public EndpointController(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @GetMapping
    public List<Endpoint> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String name) {
        if (name != null) {
            return endpointService.searchByName(name);
        }
        if (Boolean.TRUE.equals(active)) {
            return endpointService.getActive();
        }
        return endpointService.getAll();
    }

    @GetMapping("/{id}")
    public Endpoint getById(@PathVariable Long id) {
        return endpointService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Endpoint create(@RequestBody Endpoint endpoint) {
        return endpointService.create(endpoint);
    }

    @PutMapping("/{id}")
    public Endpoint update(@PathVariable Long id, @RequestBody Endpoint endpoint) {
        return endpointService.update(id, endpoint);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        endpointService.delete(id);
    }

    @ExceptionHandler(com.derbenev.monitor.exception.EndpointNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(com.derbenev.monitor.exception.EndpointNotFoundException ex) {
        return ex.getMessage();
    }
}
