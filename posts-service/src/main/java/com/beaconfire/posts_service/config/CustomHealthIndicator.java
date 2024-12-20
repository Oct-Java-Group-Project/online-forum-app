package com.beaconfire.posts_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Health health() {
        // Check the database status
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
     * Check the health of the MongoDB connection.
     */
    private boolean checkDatabaseStatus() {
        try {
            mongoTemplate.executeCommand("{ping:1}");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
