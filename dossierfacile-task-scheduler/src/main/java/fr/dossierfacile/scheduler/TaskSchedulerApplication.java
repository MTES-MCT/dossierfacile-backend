package fr.dossierfacile.scheduler;

import fr.dossierfacile.logging.async.EnableMdcAwareAsync;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"fr.dossierfacile"})
@EntityScan(basePackages = "fr.dossierfacile")
@EnableMdcAwareAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
public class TaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
    }
}
