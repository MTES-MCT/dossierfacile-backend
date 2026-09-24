package fr.dossierfacile.scheduler.tasks.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsPropertiesTest {

    @Test
    @DisplayName("Devrait privilégier les valeurs explicites par rapport aux valeurs par défaut")
    void should_prefer_explicit_values() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDefaultSourceUrl("jdbc:postgresql://default-host:5432/default_db");
        properties.setDefaultSourceUsername("default_user");
        properties.setDefaultSourcePassword("default_pwd");

        // When explicit properties are set
        properties.setSourceUrl("jdbc:postgresql://custom-host:5432/custom_db");
        properties.setSourceUsername("custom_user");
        properties.setSourcePassword("custom_pwd");
        properties.setDestUrl("jdbc:postgresql://dest-host:5432/dest_db");
        properties.setDestUsername("dest_user");
        properties.setDestPassword("dest_pwd");
        properties.setSalt("test_salt");
        properties.setDbtWebhookUrl("https://webhook.url");
        properties.setDbtWebhookToken("webhook_token");

        assertThat(properties.getSourceUrl()).isEqualTo("jdbc:postgresql://custom-host:5432/custom_db");
        assertThat(properties.getSourceUsername()).isEqualTo("custom_user");
        assertThat(properties.getSourcePassword()).isEqualTo("custom_pwd");
        assertThat(properties.getDestUrl()).isEqualTo("jdbc:postgresql://dest-host:5432/dest_db");
        assertThat(properties.getDestUsername()).isEqualTo("dest_user");
        assertThat(properties.getDestPassword()).isEqualTo("dest_pwd");
        assertThat(properties.getSalt()).isEqualTo("test_salt");
        assertThat(properties.getDbtWebhookUrl()).isEqualTo("https://webhook.url");
        assertThat(properties.getDbtWebhookToken()).isEqualTo("webhook_token");
    }

    @Test
    @DisplayName("Devrait replier sur spring.datasource si sourceUrl non spécifié")
    void should_fallback_to_default_datasource() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setDefaultSourceUrl("jdbc:postgresql://default-host:5432/default_db");
        properties.setDefaultSourceUsername("default_user");
        properties.setDefaultSourcePassword("default_pwd");

        assertThat(properties.getSourceUrl()).isEqualTo("jdbc:postgresql://default-host:5432/default_db");
        assertThat(properties.getSourceUsername()).isEqualTo("default_user");
        assertThat(properties.getSourcePassword()).isEqualTo("default_pwd");
    }

    @Test
    @DisplayName("Devrait être désactivé par défaut et activable")
    void should_default_to_disabled_and_be_toggleable() {
        AnalyticsProperties properties = new AnalyticsProperties();
        assertThat(properties.isEnabled()).isFalse();

        properties.setEnabled(true);
        assertThat(properties.isEnabled()).isTrue();
    }
}
