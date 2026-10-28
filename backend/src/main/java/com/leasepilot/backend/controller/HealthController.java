package com.leasepilot.backend.controller;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // GET /api/health -> reports whether the app and the database are reachable
    @GetMapping("/health")
    public Map<String, String> health() {
        String database;
        try (Connection connection = dataSource.getConnection()) {
            database = connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            database = "DOWN";
        }
        return Map.of("status", "UP", "database", database);
    }
}
