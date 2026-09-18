package com.derbenev.monitor.service;

import com.derbenev.monitor.exception.EndpointNotFoundException;
import com.derbenev.monitor.model.Endpoint;
import com.derbenev.monitor.repository.EndpointRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EndpointService {

    private final EndpointRepository endpointRepository;

    public EndpointService(EndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    public List<Endpoint> getAll() {
        return endpointRepository.findAll();
    }

    public List<Endpoint> getActive() {
        return endpointRepository.findByActiveTrue();
    }

    public List<Endpoint> searchByName(String name) {
        return endpointRepository.findByNameContainingIgnoreCase(name);
    }

    public Endpoint getById(Long id) {
        return endpointRepository.findById(id)
                .orElseThrow(() -> new EndpointNotFoundException(id));
    }

    public Endpoint create(Endpoint endpoint) {
        endpoint.setId(null);
        endpoint.setCreatedAt(LocalDateTime.now());
        return endpointRepository.save(endpoint);
    }

    public Endpoint update(Long id, Endpoint update) {
        Endpoint existing = getById(id);
        existing.setName(update.getName());
        existing.setUrl(update.getUrl());
        existing.setActive(update.isActive());
        return endpointRepository.save(existing);
    }

    public void delete(Long id) {
        if (!endpointRepository.existsById(id)) {
            throw new EndpointNotFoundException(id);
        }
        endpointRepository.deleteById(id);
    }
}
