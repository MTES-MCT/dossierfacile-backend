package fr.dossierfacile.api.pdfgenerator.configuration;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class Config {
    @Bean
    public Gson gson() {
        return new Gson();
    }

    @Bean
    public String configPDFBox() {
        return System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
