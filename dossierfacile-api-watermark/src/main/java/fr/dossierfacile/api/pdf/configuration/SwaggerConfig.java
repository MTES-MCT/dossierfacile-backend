package fr.dossierfacile.api.pdf.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("API")
                .select()
                .apis(RequestHandlerSelectors.basePackage("fr.dossierfacile.api.pdf.controller"))
                .paths(PathSelectors.ant("/api/document/**"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "API Watermark",
                "<h3>Cette API est ouverte, elle permet de protéger ses documents PDF par l'ajout d'un filigrane</h3>\n" +
                        "<div> Les fichiers acceptés sont les : png, jpg et pdf</div>" +
                        "<div> Le fichier en sortie est un PDF</div>",
                "1.0",
                "",
                ApiInfo.DEFAULT_CONTACT,
                "MIT", "", Collections.emptyList());
    }
}
