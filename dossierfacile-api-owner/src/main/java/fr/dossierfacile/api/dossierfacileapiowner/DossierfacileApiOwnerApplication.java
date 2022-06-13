package fr.dossierfacile.api.dossierfacileapiowner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@ComponentScan(basePackages = "fr.dossierfacile")
@EnableAsync
@EnableScheduling
@Slf4j
public class DossierfacileApiOwnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DossierfacileApiOwnerApplication.class, args);
	}

}
