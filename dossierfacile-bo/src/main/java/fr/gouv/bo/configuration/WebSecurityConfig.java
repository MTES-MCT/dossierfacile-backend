package fr.gouv.bo.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dossierfacile.common.utils.LocalDateTimeTypeAdapter;
import fr.gouv.bo.security.QuotaAccessVoter;
import lombok.AllArgsConstructor;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableWebSecurity
@EnableAsync
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@AllArgsConstructor
public class WebSecurityConfig {

    private QuotaAccessVoter quotaAccessVoter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(new BOConnectionContextFilter(), FilterSecurityInterceptor.class)
                .requiresChannel()
                .anyRequest()
                .requiresSecure()
                .and()
                .csrf()
                .csrfTokenRepository(csrfTokenRepository())
                .and()
                .headers()
                .contentTypeOptions()
                .and()
                .xssProtection()
                .and()
                .cacheControl()
                .and()
                .contentSecurityPolicy("frame-ancestors *.dossierfacile.fr; frame-src *.dossierfacile.fr; child-src 'none'; upgrade-insecure-requests; default-src 'none'; script-src 'self'; style-src 'self' fonts.googleapis.com 'unsafe-inline'; object-src *.dossierfacile.fr; img-src 'self' *.dossierfacile.fr data:; font-src 'self' fonts.gstatic.com; connect-src *.dossierfacile.fr; base-uri 'self'; media-src 'none'; worker-src *.dossierfacile.fr; manifest-src 'none';")
                .and()
                .httpStrictTransportSecurity().disable()// Scalingo force https and add this policy
                .frameOptions()
                .sameOrigin()
                .and()
                .formLogin()
                .disable()
                .httpBasic()
                .disable()
                .authorizeRequests()
                .accessDecisionManager(accessDecisionManager())
                .antMatchers("/login", "/login/auth/**", "/login/oauth2/**", "/actuator/health", "/assets/public/**")
                .permitAll()
                .antMatchers("/bo/userApi", "/bo/userApi/**", "/bo/admin", "/bo/admin/**", "/bo/statistic/admin", "/bo/timeServeTenant", "/bo/users", "/bo/users/**")
                .hasRole("ADMIN")
                .antMatchers("/bo/**", "/bo", "/documents/**")
                .hasAnyRole("ADMIN", "OPERATOR")
                .anyRequest()
                .authenticated()
                .and()
                .oauth2Login()
                .loginPage("/login")
                .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                .and()
                .logout()
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "JWT", "_csrf");

        return http.build();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        List<AccessDecisionVoter<?>> decisionVoters = Arrays.asList(
                new WebExpressionVoter(),
                quotaAccessVoter
        );
        return new UnanimousBased(decisionVoters);
    }


    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setSessionAttributeName("_csrf");
        return repository;
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Mailer-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        return builder.create();
    }

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
