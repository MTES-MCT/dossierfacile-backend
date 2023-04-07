package fr.dossierfacile.api.pdfgenerator.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dossierfacile.common.utils.LocalDateTimeTypeAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;


@Configuration
@EnableJpaAuditing
public class Config {

    @Bean
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        return builder.create();
    }

    @Bean
    public String configPDFBox() {
        return System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return (() -> Optional.of("pdf-generator"));
    }
}
