package fr.dossierfacile.api.pdf.configuration;


import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    private static final String LI = "<li>";
    private static final String LI_CLOSE = "</li>";

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
                "DossierFacile REST Document API",
                "<h3>Description of the api</h3>\n" +
                        "<div>\n" +
                        "    <ul>\n" +
                        LI + "\n" +
                        "       TODO: (PUT HERE YOUR DESCRIPTION).\n" +
                        LI_CLOSE + "\n" +
                        "    </ul>\n" +
                        "</div>",
                "1.0",
                "",
                ApiInfo.DEFAULT_CONTACT,
                "", "", Collections.emptyList());
    }
}
