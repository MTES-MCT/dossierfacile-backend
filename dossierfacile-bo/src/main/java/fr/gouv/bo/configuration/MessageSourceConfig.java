package fr.gouv.bo.configuration;

import fr.dossierfacile.common.config.ApiVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {
    @Value("${application.api.version}")
    private Integer apiVersion;

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(ApiVersion.V3.is(apiVersion) ? "messages_v3" : "messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
