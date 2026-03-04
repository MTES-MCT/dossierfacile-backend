package fr.dossierfacile.api.front.repository;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@EnableJpaAuditing
public class JpaTestApplication {
}
