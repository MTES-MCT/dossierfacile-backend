package fr.gouv.owner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"fr.gouv", "fr.dossierfacile"})
@EnableJpaRepositories(basePackages = {"fr.gouv", "fr.dossierfacile"})
@EntityScan(basePackages = "fr.dossierfacile")
public class OwnerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(OwnerApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OwnerApplication.class);
    }

}
