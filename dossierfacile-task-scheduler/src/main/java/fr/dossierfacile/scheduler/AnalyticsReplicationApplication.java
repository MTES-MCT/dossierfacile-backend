package fr.dossierfacile.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Application Spring Boot dédiée à la réplication Analytics.
 */
@Profile("analytics-replication")
@Configuration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "fr.dossierfacile.scheduler.tasks.analytics",
        "fr.dossierfacile.scheduler.cli",
        "fr.dossierfacile.logging"
})
public class AnalyticsReplicationApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AnalyticsReplicationApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setAdditionalProfiles("analytics-replication");
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (!argList.contains("--run-task=replicate-analytics")) {
            argList.add("--run-task=replicate-analytics");
        }
        app.run(argList.toArray(new String[0]));
    }
}
