package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        ResourceServerConfig.class,
        CustomRestExceptionHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "dossierfacile.common.global.exception.handler=true",
        "resource.server.config.csp=default-src 'self'"
})
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApartmentSharingService apartmentSharingService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID TEST_TOKEN = UUID.fromString("186ed480-968b-4bcd-aaab-afa747b953da");
    private static final String VALID_TRIGRAM = "NOM";
    private static final String INVALID_TRIGRAM = "BAD";

    @Test
    void shouldReturn200WhenLinkExists() throws Exception {
        // When & Then
        mockMvc.perform(head("/api/application/full/{token}", TEST_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenLinkDoesNotExist() throws Exception {
        // Given
        UUID nonExistentToken = UUID.randomUUID();
        doThrow(new fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException(
                        nonExistentToken.toString()))
                .when(apartmentSharingService).linkExists(eq(nonExistentToken), eq(true));

        // When & Then
        mockMvc.perform(head("/api/application/full/{token}", nonExistentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WhenValidTrigramProvided() throws Exception {
        // Given
        ApplicationModel applicationModel = new ApplicationModel();
        when(apartmentSharingService.full(eq(TEST_TOKEN), eq(VALID_TRIGRAM)))
                .thenReturn(applicationModel);

        // When & Then
        mockMvc.perform(get("/api/application/full/{token}", TEST_TOKEN)
                        .header("X-Tenant-Trigram", VALID_TRIGRAM))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldReturn403WhenInvalidTrigramProvided() throws Exception {
        // Given
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .build();
        Tenant tenant = Tenant.builder()
                .id(1L)
                .lastName("Nom") // This will generate trigram "NOM"
                .apartmentSharing(apartmentSharing)
                .build();
        apartmentSharing.setTenants(Collections.singletonList(tenant));

        when(apartmentSharingService.full(eq(TEST_TOKEN), eq(INVALID_TRIGRAM)))
                .thenThrow(new fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException(
                        "Trigram does not match any tenant for this application"));

        // When & Then
        mockMvc.perform(get("/api/application/full/{token}", TEST_TOKEN)
                        .header("X-Tenant-Trigram", INVALID_TRIGRAM))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenApplicationTokenNotFound() throws Exception {
        // Given
        UUID nonExistentToken = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(apartmentSharingService.full(eq(nonExistentToken), eq(VALID_TRIGRAM)))
                .thenThrow(new fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException(
                        nonExistentToken.toString()));

        // When & Then
        mockMvc.perform(get("/api/application/full/{token}", nonExistentToken)
                        .header("X-Tenant-Trigram", VALID_TRIGRAM))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn429WhenLinkIsBlockedByBruteForceProtection() throws Exception {
        // Given
        // Simulates scenario where brute-force.max-attempts has been exceeded
        when(apartmentSharingService.full(eq(TEST_TOKEN), eq(INVALID_TRIGRAM)))
                .thenThrow(new fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException(
                        "Too many failed attempts. Link is temporarily blocked."));

        // When & Then
        mockMvc.perform(get("/api/application/full/{token}", TEST_TOKEN)
                        .header("X-Tenant-Trigram", INVALID_TRIGRAM))
                .andExpect(status().isTooManyRequests());
    }
}

