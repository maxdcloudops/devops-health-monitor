package com.derbenev.monitor.repository;

import com.derbenev.monitor.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long>{

    List<Endpoint> findByActivetrue();
    List<Endpoint> findByNameContainingIgnoreCase(String name);
}
