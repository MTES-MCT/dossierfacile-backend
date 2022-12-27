package fr.gouv.bo.configuration;

import com.google.gson.Gson;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextListener;

import java.util.concurrent.Executor;

@Configuration
@EnableWebSecurity
@EnableAsync
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
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
                .contentSecurityPolicy("frame-ancestors 'none'; frame-src 'none'; child-src 'none'; upgrade-insecure-requests; default-src 'none'; script-src 'self'; style-src 'self' fonts.googleapis.com 'unsafe-inline'; object-src 'none'; img-src 'self' data:; font-src 'self' fonts.gstatic.com; connect-src *.dossierfacile.fr; base-uri 'self'; form-action 'none'; media-src 'none'; worker-src 'none'; manifest-src 'none'; prefetch-src 'none';")
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
                .antMatchers("/", "/login", "/login/auth/**", "/login/oauth2/**,", "/actuator/health", "/assets/public/**")
                .permitAll()
                .antMatchers("/bo/userApi", "/bo/userApi/**", "/bo/admin", "/bo/admin/**", "/bo/statistic/admin", "/bo/timeServeTenant", "/bo/deleteAccount", "/bo/sleepMode", "/bo/create/admin").access("hasAnyRole('ROLE_ADMIN')")
                .antMatchers("/bo/nextApplicationQuickly").access("hasAnyRole('ROLE_OPERATOR','ROLE_ADMIN')")
                .antMatchers("/bo/**", "/bo").access("hasAnyRole('ROLE_OPERATOR','ROLE_ADMIN','ROLE_OPERATOR_AWAY')")
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
        // @formatter:on
    }

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository httpSessionOAuth2AuthorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setSessionAttributeName("_csrf");
        return repository;
    }

    @Autowired
    public void registerGlobalAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
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
        return new Gson();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
