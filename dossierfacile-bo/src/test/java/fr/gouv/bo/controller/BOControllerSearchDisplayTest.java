package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.TestBOApplication;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.service.BOTenantResolver;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BOController.class)
@ContextConfiguration(classes = {TestBOApplication.class, BOControllerSearchDisplayTest.TestSecurityConfig.class})
@ActiveProfiles("test")
@MockitoBean(types = {UserService.class, DocumentService.class, PartnerCallBackService.class,
        BOApplicationAccessService.class, BOTenantResolver.class, ClientRegistrationRepository.class})
class BOControllerSearchDisplayTest {

    @Configuration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void displaysAllGuarantorEmailsForASingleMatchingTenant() throws Exception {
        Tenant tenant = Tenant.builder().id(42L).email("tenant@example.com")
                .guarantors(List.of(
                        Guarantor.builder().email("guarantor@example.com").build(),
                        Guarantor.builder().email("second@example.com").build(),
                        Guarantor.builder().build()))
                .build();
        when(tenantService.getTenantByIdOrEmail(eq("guarantor@example.com"), any()))
                .thenReturn(new PageImpl<>(List.of(tenant), PageRequest.of(0, 20), 1));

        String html = mockMvc.perform(get("/bo/searchTenant").param("email", "guarantor@example.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html);

        assertThat(document.select("#userTable thead td")).extracting(Element::text)
                .contains("Emails des garants");
        assertThat(document.select("#userTable tbody tr")).hasSize(1);
        assertThat(document.select("#userTable tbody td:last-child li")).extracting(Element::text)
                .containsExactly("guarantor@example.com", "second@example.com");
        assertThat(document.selectFirst("#userTable a").attr("href")).isEqualTo("/bo/tenant/42");
    }
}
