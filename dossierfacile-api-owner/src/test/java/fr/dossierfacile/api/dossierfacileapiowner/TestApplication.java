package fr.dossierfacile.api.dossierfacileapiowner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootApplication
@ActiveProfiles(value = "test")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(DossierfacileApiOwnerApplication.class, args);
    }

    @Bean
    @Profile("test")
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().anyRequest();
    }
}
