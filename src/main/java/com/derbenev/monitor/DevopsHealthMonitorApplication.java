package com.derbenev.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevopsHealthMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevopsHealthMonitorApplication.class, args);
    }

}
