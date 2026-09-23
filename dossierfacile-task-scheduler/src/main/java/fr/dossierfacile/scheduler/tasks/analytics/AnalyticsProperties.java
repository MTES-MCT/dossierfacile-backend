package fr.dossierfacile.scheduler.tasks.analytics;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "analytics.replication")
public class AnalyticsProperties {

    @Value("${spring.datasource.url:}")
    private String defaultSourceUrl;

    @Value("${spring.datasource.username:}")
    private String defaultSourceUsername;

    @Value("${spring.datasource.password:}")
    private String defaultSourcePassword;

    @Value("${analytics.replication.enabled:false}")
    private boolean enabled = false;

    private String sourceUrl;
    private String sourceUsername;
    private String sourcePassword;

    private String destUrl;
    private String destUsername;
    private String destPassword;

    private String salt;
    private String dbtWebhookUrl;
    private String dbtWebhookToken;

    public String getSourceUrl() {
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            return sourceUrl;
        }
        return defaultSourceUrl;
    }

    public String getSourceUsername() {
        if (sourceUsername != null && !sourceUsername.isBlank()) {
            return sourceUsername;
        }
        return defaultSourceUsername;
    }

    public String getSourcePassword() {
        if (sourcePassword != null && !sourcePassword.isBlank()) {
            return sourcePassword;
        }
        return defaultSourcePassword;
    }
}
