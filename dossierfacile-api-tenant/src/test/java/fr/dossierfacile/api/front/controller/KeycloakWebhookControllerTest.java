package fr.dossierfacile.api.front.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.front.domain.service.FindOrCreateTenantDomainService;
import fr.dossierfacile.api.front.domain.service.TenantSynchronizationDomainService;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.KeycloakWebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KeycloakWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FindOrCreateTenantDomainService findOrCreateTenantDomainService;

    @Mock
    private TenantSynchronizationDomainService tenantSynchronizationDomainService;

    @InjectMocks
    private KeycloakWebhookController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        ReflectionTestUtils.setField(controller, "expectedToken", "my-secret-keycloak-token");
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        KeycloakWebhookEvent event = new KeycloakWebhookEvent();
        event.setType("UPDATE_EMAIL");
        event.setUserId("kc-123");

        mockMvc.perform(post("/api/webhook/keycloak/user-sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(findOrCreateTenantDomainService, tenantSynchronizationDomainService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        KeycloakWebhookEvent event = new KeycloakWebhookEvent();
        event.setType("LOGIN");
        event.setUserId("kc-123");

        mockMvc.perform(post("/api/webhook/keycloak/user-sync")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(findOrCreateTenantDomainService, tenantSynchronizationDomainService);
    }

    @Test
    void shouldReturnOkWithValidBearerToken() throws Exception {
        KeycloakWebhookEvent event = new KeycloakWebhookEvent();
        event.setType("LOGIN");
        event.setUserId("kc-123");
        event.setEmail("test@test.fr");

        when(findOrCreateTenantDomainService.findOrCreateTenant(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/webhook/keycloak/user-sync")
                        .header("Authorization", "Bearer my-secret-keycloak-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        verify(findOrCreateTenantDomainService, times(1)).findOrCreateTenant(any(), any());
        verify(tenantSynchronizationDomainService, times(1)).synchronizeTenant(any(), any());
    }

    @Test
    void shouldReturnOkWithValidRawToken() throws Exception {
        KeycloakWebhookEvent event = new KeycloakWebhookEvent();
        event.setType("LOGIN");
        event.setUserId("kc-123");
        event.setEmail("test@test.fr");

        when(findOrCreateTenantDomainService.findOrCreateTenant(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/webhook/keycloak/user-sync")
                        .header("Authorization", "my-secret-keycloak-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        verify(findOrCreateTenantDomainService, times(1)).findOrCreateTenant(any(), any());
        verify(tenantSynchronizationDomainService, times(1)).synchronizeTenant(any(), any());
    }

    @Test
    void shouldParseAttributes() throws Exception {
        KeycloakWebhookEvent event = new KeycloakWebhookEvent();
        event.setType("LOGIN");
        event.setUserId("kc-123");
        
        java.util.Map<String, java.util.List<String>> attributes = new java.util.HashMap<>();
        attributes.put("france-connect", java.util.List.of("true"));
        attributes.put("france-connect-sub", java.util.List.of("fc-sub-abc"));
        attributes.put("birthcountry", java.util.List.of("France"));
        attributes.put("birthplace", java.util.List.of("Paris"));
        attributes.put("birthdate", java.util.List.of("1990-01-01"));
        attributes.put("firstName", java.util.List.of("Jean"));
        attributes.put("lastName", java.util.List.of("Dupont"));
        attributes.put("email", java.util.List.of("test@test.fr"));
        attributes.put("username", java.util.List.of("jean.dupont"));
        attributes.put("gender", java.util.List.of("male"));
        event.setAttributes(attributes);

        org.mockito.ArgumentCaptor<KeycloakUser> captor = org.mockito.ArgumentCaptor.forClass(KeycloakUser.class);
        when(findOrCreateTenantDomainService.findOrCreateTenant(captor.capture(), any())).thenReturn(null);

        mockMvc.perform(post("/api/webhook/keycloak/user-sync")
                        .header("Authorization", "Bearer my-secret-keycloak-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        KeycloakUser keycloakUser = captor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(keycloakUser.isFranceConnect());
        org.junit.jupiter.api.Assertions.assertEquals("fc-sub-abc", keycloakUser.getFranceConnectSub());
        org.junit.jupiter.api.Assertions.assertEquals("France", keycloakUser.getFranceConnectBirthCountry());
        org.junit.jupiter.api.Assertions.assertEquals("Paris", keycloakUser.getFranceConnectBirthPlace());
        org.junit.jupiter.api.Assertions.assertEquals("1990-01-01", keycloakUser.getFranceConnectBirthDate());
        org.junit.jupiter.api.Assertions.assertEquals("Jean", keycloakUser.getGivenName());
        org.junit.jupiter.api.Assertions.assertEquals("Dupont", keycloakUser.getFamilyName());
        org.junit.jupiter.api.Assertions.assertEquals("test@test.fr", keycloakUser.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("jean.dupont", keycloakUser.getPreferredUsername());
        org.junit.jupiter.api.Assertions.assertEquals("jean.dupont", keycloakUser.getComputedPreferredUserName());
        org.junit.jupiter.api.Assertions.assertEquals("male", keycloakUser.getGender());
        org.junit.jupiter.api.Assertions.assertEquals("9d50bd488970d998c5d5c4c139b7d92dd4d3ef16a2dfe57f649d91a49e3ff021", keycloakUser.getFcHash());

        verify(tenantSynchronizationDomainService, times(1)).synchronizeTenant(any(), any());
    }
}
