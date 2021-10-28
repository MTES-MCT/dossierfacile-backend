package fr.dossierfacile.api.front.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    private static final String URL = "www.example.com";
    private static final String LI = "<li>";
    private static final String LI_CLOSE = "</li>";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("API")
                .select()
                .apis(RequestHandlerSelectors.basePackage("fr.dossierfacile.api.front.controller"))
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .apiInfo(apiInfo());
    }

    @Bean
    public Docket apiDossierFacileConnect() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("API DFC")
                .select()
                .apis(RequestHandlerSelectors.basePackage("fr.dossierfacile.api.front.dfc.controller"))
                .paths(PathSelectors.ant("/dfc/**"))
                .build()
                .apiInfo(apiDossierFacileConnectInfo());
    }

    @Bean
    public Docket apiPartner() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("API Partner")
                .select()
                .apis(RequestHandlerSelectors.basePackage("fr.dossierfacile.api.front.partner.controller"))
                .paths(PathSelectors.ant("/api-partner/**"))
                .build()
                .apiInfo(apiPartnerInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "DossierFacile REST API",
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

    private ApiInfo apiDossierFacileConnectInfo() {
        return new ApiInfo(
                "DossierFacile REST API DossierFacileConnect (DFC)",
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

    private ApiInfo apiPartnerInfo() {
        return new ApiInfo(
                "DossierFacile REST API Partner",
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
