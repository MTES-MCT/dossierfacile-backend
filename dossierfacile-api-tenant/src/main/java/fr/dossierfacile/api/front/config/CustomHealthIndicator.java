package fr.dossierfacile.api.front.config;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up()
                .withDetail("Service", "Running")
                .withDetail("Error", "No Error- Healthy status")
                .withException(new RuntimeException());
    }
}