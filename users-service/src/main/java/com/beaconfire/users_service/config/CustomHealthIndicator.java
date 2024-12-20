package com.beaconfire.users_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        // Check multiple aspects of the application's health
        boolean databaseStatus = checkDatabaseStatus();

        if (databaseStatus) {
            return Health.up()
                    .withDetail("Server Status", "All Systems Operational")
                    .withDetail("Database Status", "Connected")
                    .build();
        } else {
            return Health.down()
                    .withDetail("Server Status", "Degraded")
                    .withDetail("Database Status", "Disconnected")
                    .build();
        }
    }

    /**
     * Simulate a database health check.
     * Replace this with actual database connection health logic.
     */
    private boolean checkDatabaseStatus() {
        try {
            // Execute a lightweight query to check database connectivity
            jdbcTemplate.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            return false; // Database is unreachable
        }
    }
}
