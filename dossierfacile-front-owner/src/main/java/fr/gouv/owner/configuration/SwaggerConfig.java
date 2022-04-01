package fr.gouv.owner.configuration;


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
                .select()
                .apis(RequestHandlerSelectors.basePackage("fr.gouv.owner.rest"))
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "DossierFacile REST API",
                "<h3>Description of the api</h3>\n" +
                        "<div>\n" +
                        "    <ul>\n" +
                        LI + "\n" +
                        "            First we must authenticate in the api to use any of the methods exposed in this documentation.\n" +
                        LI_CLOSE + "\n" +
                        LI + "\n" +
                        "            We must start using the endpoint, create a apartment sharing (application), where the first tenant will be responsible for the application. This endpoint returns the identifiers of each of the tenants created.\n" +
                        LI_CLOSE + "\n" +
                        LI + "\n" +
                        "            Then using the identifiers returned in the previous endpoint we upload the files of each of the tenants separately, when this step is completed, then the application will be complete and ready for our operators to review their application in the system.\n" +
                        LI_CLOSE + "\n" +
                        LI + "\n" +
                        "            The application can be rejected or accepted, in both cases you will be notified, for this you need to enable an endpoint for locatio to send the data.\n" +
                        LI_CLOSE + "\n" +
                        LI + "\n" +
                        "            In case the application is rejected, because an incorrect document was detected, for example, the operator will send a feedback message that you can answer, in addition you can change the file indicated.\n" +
                        LI_CLOSE + "\n" +
                        LI + "\n" +
                        "            Once you have modified the files indicated by the operator, the entire process will begin again, until the operator validates your application satisfactorily\n" +
                        LI_CLOSE + "\n" +
                        "    </ul>\n" +
                        "</div>",
                "1.0",
                URL,
                new Contact("Harlow Fres", URL, "contact@example.com"),
                "License of API", URL, Collections.emptyList());
    }

}
