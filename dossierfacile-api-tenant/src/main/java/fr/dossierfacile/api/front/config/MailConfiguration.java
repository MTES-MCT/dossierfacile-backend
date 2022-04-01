package fr.dossierfacile.api.front.config;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.resource.ApiVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {
    @Value("${spring.mail.username}")
    private String usernameForCommonAccount;
    @Value("${spring.mail.password}")
    private String passwordForCommonAccount;

    @Value("${mailjet.api.key.for.warning.accounts}")
    private String apiKeyForWarningsAccount;
    @Value("${mailjet.secret.key.for.warning.accounts}")
    private String secretKeyForWarningsAccount;

    @Qualifier("common_account")
    @Bean
    public MailjetClient commonAccount() {
        return new MailjetClient(usernameForCommonAccount, passwordForCommonAccount, new ClientOptions(ApiVersion.V3_1));
    }

    @Qualifier("warnings_account")
    @Bean
    public MailjetClient warningsAccount() {
        return new MailjetClient(apiKeyForWarningsAccount, secretKeyForWarningsAccount, new ClientOptions(ApiVersion.V3_1));
    }
}
