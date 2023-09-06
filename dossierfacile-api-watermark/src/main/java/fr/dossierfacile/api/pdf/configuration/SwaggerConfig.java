package fr.dossierfacile.api.pdf.configuration;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI watermarkOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("API Watermark")
                        .description("<h3>Cette API est ouverte, elle permet de protéger ses documents PDF par l'ajout d'un filigrane</h3>"
                                + "<div> Les fichiers acceptés sont les : png, jpg et pdf</div>"
                                + "<div> Le fichier en sortie est un PDF</div>")
                        .version("v0.0.1")
                        .license(new License().name("MIT")));
    }
}
