package com.finance.infrastructure.web.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness (/health) answers whenever the process is running and never touches the database, so an uptime monitor or the
 * keep-awake script can call it all day without waking a sleeping free-tier database. Readiness (/health/ready) also checks
 * the database. AI is deliberately not part of either: the product has to work with it switched off or failing.
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.of(Map.of("status", "UP"));
    }

    @GetMapping("/api/v1/health/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(ApiResponse.of(Map.of("status", "UP", "database", "UP")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.of(Map.of("status", "DOWN", "database", "DOWN")));
        }
    }
}
