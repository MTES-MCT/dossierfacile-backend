package fr.dossierfacile.common.config.xss;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonXssConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xssSanitizerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(String.class, new XssStringJsonDeserializer());
            builder.modulesToInstall(module);
        };
    }
}
