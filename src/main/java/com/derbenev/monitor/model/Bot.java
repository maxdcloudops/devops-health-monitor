package com.derbenev.monitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bots")
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String broker;

    @Column(nullable = false)
    private String strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status = BotStatus.STOPPED;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;
}
