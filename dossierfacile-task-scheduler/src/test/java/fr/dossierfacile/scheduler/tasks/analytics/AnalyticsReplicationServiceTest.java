package fr.dossierfacile.scheduler.tasks.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsReplicationServiceTest {

    @Test
    @DisplayName("Devrait lever IllegalArgumentException si le sel est manquant ou vide")
    void should_throw_if_salt_is_missing() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setSalt("");
        AnalyticsReplicationService service = new AnalyticsReplicationService(properties);

        assertThatThrownBy(service::replicateAll)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ANALYTICS_SALT must be configured and not blank");
    }

    @Test
    @DisplayName("Devrait lever IllegalArgumentException si l'URL source est manquante")
    void should_throw_if_source_url_is_missing() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setSalt("my-salt");
        properties.setSourceUrl("");
        AnalyticsReplicationService service = new AnalyticsReplicationService(properties);

        assertThatThrownBy(service::replicateAll)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source database URL must be configured");
    }

    @Test
    @DisplayName("Devrait lever IllegalArgumentException si l'URL destination est manquante")
    void should_throw_if_dest_url_is_missing() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setSalt("my-salt");
        properties.setSourceUrl("jdbc:postgresql://localhost:5432/source_db");
        properties.setDestUrl("");
        AnalyticsReplicationService service = new AnalyticsReplicationService(properties);

        assertThatThrownBy(service::replicateAll)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination database URL must be configured");
    }

    @Test
    @DisplayName("Devrait lever SQLException si la connexion source ou destination échoue")
    void should_throw_sql_exception_on_connection_failure() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setSalt("my-salt");
        properties.setSourceUrl("jdbc:postgresql://invalid-host:5432/source_db");
        properties.setDestUrl("jdbc:postgresql://invalid-host:5432/dest_db");
        AnalyticsReplicationService service = new AnalyticsReplicationService(properties);

        assertThatThrownBy(service::replicateAll)
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("Devrait lever IllegalStateException si l'URL source et destination sont identiques")
    void should_throw_if_source_and_dest_urls_are_identical() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setSalt("my-salt");
        properties.setSourceUrl("jdbc:postgresql://localhost:5432/same_db");
        properties.setDestUrl("jdbc:postgresql://localhost:5432/same_db");
        AnalyticsReplicationService service = new AnalyticsReplicationService(properties);

        assertThatThrownBy(service::replicateAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Destination database URL is identical to Source database URL");
    }
}
