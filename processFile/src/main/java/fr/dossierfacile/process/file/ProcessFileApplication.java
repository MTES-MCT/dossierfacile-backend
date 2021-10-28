package fr.dossierfacile.process.file;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@ComponentScan(basePackages = "fr.dossierfacile")
public class ProcessFileApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ProcessFileApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
