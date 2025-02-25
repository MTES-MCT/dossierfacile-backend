package fr.dossierfacile.api.front.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

// This configuration add a log when the LogAggregationFilter is not registered
@Configuration
public class LogAggregationConfig {

    private static final Logger logger = LoggerFactory.getLogger(LogAggregationConfig.class);

    @Value("${logging.logstash.destination:}")
    private String logstashDestination;

    @PostConstruct
    public void checkLogstashDestination() {
        if (logstashDestination.isEmpty()) {
            logger.warn("Logstash destination is not set. LogAggregationFilter will not be registered.");
        }
        else {
            logger.warn("Logstash destination is set. LogAggregationFilter will be registered.");
        }
    }
}