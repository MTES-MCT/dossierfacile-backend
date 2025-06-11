package fr.dossierfacile.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class ApiClientConfig {
    @Bean
    HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }
}
