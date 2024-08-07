package fr.dossierfacile.api.dossierfacileapiowner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@EnableAsync
@EnableScheduling
public class DossierfacileApiOwnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DossierfacileApiOwnerApplication.class, args);
	}

}
