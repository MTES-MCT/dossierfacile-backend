package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.junit.jupiter.api.Test;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

    @MockitoBean
    private ApartmentSharingService apartmentSharingService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final UUID TEST_TOKEN = UUID.fromString("186ed480-968b-4bcd-aaab-afa747b953da");
    private static final String VALID_TRIGRAM = "NOM";
    private static final String INVALID_TRIGRAM = "BAD";

    @Test
    void shouldReturn200WhenLinkExists() throws Exception {
        // When & Then
        mockMvc.perform(head("/api/application/full/{token}", TEST_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/application/full/{token}", TEST_TOKEN)
                        .header("X-Tenant-Trigram", VALID_TRIGRAM))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenTrigramIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/application/full/{token}", TEST_TOKEN))
                .andExpect(status().isBadRequest());
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


    @Nested
    class GetFullPdfForLoggedTenantTests {

        record GetFullPdfForLoggedTenantTestParameter() {
        }

        static List<Arguments> provideGetFullPdfForLoggedTenantParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            ApartmentSharing apartmentSharing = new ApartmentSharing();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .apartmentSharing(apartmentSharing)
                    .build();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("PDF content".getBytes(), 0, "PDF content".getBytes().length);
            FullFolderFile validPdfFile = new FullFolderFile(outputStream, "test.pdf");

            ByteArrayOutputStream emptyOutputStream = new ByteArrayOutputStream();
            FullFolderFile emptyPdfFile = new FullFolderFile(emptyOutputStream, "test.pdf");

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when valid tenant and PDF exists",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant)).thenReturn(validPdfFile);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when PDF file is empty",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant)).thenReturn(emptyPdfFile);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when apartment sharing not found",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant))
                                                    .thenThrow(new ApartmentSharingNotFoundException("No apartment sharing found"));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 409 when PDF is not yet available",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    409,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant))
                                                    .thenThrow(new IllegalStateException("PDF not ready"));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 409 when file not found",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    409,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant))
                                                    .thenThrow(new FileNotFoundException("File not found"));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 500 when IO exception occurs",
                            new ControllerParameter<>(
                                    new GetFullPdfForLoggedTenantTestParameter(),
                                    500,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        try {
                                            when(apartmentSharingService.downloadFullPdfForTenant(tenant))
                                                    .thenThrow(new IOException("IO error"));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetFullPdfForLoggedTenantParameters")
        void parameterizedTests(ControllerParameter<GetFullPdfForLoggedTenantTestParameter> parameter) throws Exception {
            var mockMvcRequestBuilder = get("/api/application/fullPdf")
                    .contentType("application/pdf");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class PostFullPdfForLoggedTenantTests {

        record PostFullPdfForLoggedTenantTestParameter() {
        }

        static List<Arguments> providePostFullPdfForLoggedTenantParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .apartmentSharing(apartmentSharing)
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 202 when PDF creation is triggered",
                            new ControllerParameter<>(
                                    new PostFullPdfForLoggedTenantTestParameter(),
                                    202,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doNothing().when(apartmentSharingService).createFullPdfForTenant(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when apartment sharing not found",
                            new ControllerParameter<>(
                                    new PostFullPdfForLoggedTenantTestParameter(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new ApartmentSharingNotFoundException("No apartment sharing found"))
                                                .when(apartmentSharingService).createFullPdfForTenant(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 417 when apartment sharing has unexpected error",
                            new ControllerParameter<>(
                                    new PostFullPdfForLoggedTenantTestParameter(),
                                    417,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new ApartmentSharingUnexpectedException())
                                                .when(apartmentSharingService).createFullPdfForTenant(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 500 when generic exception occurs",
                            new ControllerParameter<>(
                                    new PostFullPdfForLoggedTenantTestParameter(),
                                    500,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new RuntimeException("Generic error"))
                                                .when(apartmentSharingService).createFullPdfForTenant(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("providePostFullPdfForLoggedTenantParameters")
        void parameterizedTests(ControllerParameter<PostFullPdfForLoggedTenantTestParameter> parameter) throws Exception {
            var mockMvcRequestBuilder = post("/api/application/fullPdf")
                    .contentType("application/pdf");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}