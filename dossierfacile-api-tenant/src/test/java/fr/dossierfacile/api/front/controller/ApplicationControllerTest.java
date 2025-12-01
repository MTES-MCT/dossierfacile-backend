
package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(ApplicationController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"dossierfacile.common.global.exception.handler=true"})
public class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private static ApartmentSharingService apartmentSharingService;

    @MockBean
    private static AuthenticationFacade authenticationFacade;

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

