package fr.dossierfacile.api.front.controller;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.MethodSecurityConfig;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.MailSentLimitException;
import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.service.interfaces.ProcessingCapacityService;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TenantController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, TenantMapperImpl.class, ResourceServerConfig.class, MethodSecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"dossierfacile.common.global.exception.handler=true"})
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private ProcessingCapacityService processingCapacityService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    // Référence statique à l’instance courante du test
    private static TenantControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @Nested
    class ProfileTest {

        record ProfileTestParameter(MultiValueMap<String, String> params) {
        }

        static List<Arguments> provideProfileParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenantWithoutCampaign = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .status(TenantFileStatus.VALIDATED)
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(Collections.singletonList(tenantWithoutCampaign));

            var emptyParams = new LinkedMultiValueMap<String, String>();
            var emptyAcquisitionData = AcquisitionData.builder().build();

            var utmParams = new LinkedMultiValueMap<String, String>();
            utmParams.add("campaign", "utm_campaign");
            utmParams.add("source", "utm_source");
            utmParams.add("medium", "utm_medium");

            var utmAcquisitionData = AcquisitionData.builder()
                    .campaign("utm_campaign")
                    .medium("utm_medium")
                    .source("utm_source")
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    new ProfileTestParameter(emptyParams),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new ProfileTestParameter(emptyParams),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant(emptyAcquisitionData)).thenReturn(tenantWithoutCampaign);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.id").value(tenantWithoutCampaign.getId()),
                                            jsonPath("$.email").value(tenantWithoutCampaign.getEmail())
                                    )
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed and Utm campaign",
                            new ControllerParameter<>(
                                    new ProfileTestParameter(utmParams),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant(utmAcquisitionData)).thenReturn(tenantWithoutCampaign);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.id").value(tenantWithoutCampaign.getId()),
                                            jsonPath("$.email").value(tenantWithoutCampaign.getEmail())
                                    )
                            )
                    ),
                    Pair.of("Should respond 403 when user is not verified",
                            new ControllerParameter<>(
                                    new ProfileTestParameter(utmParams),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new AccessDeniedException("User not verified")).when(self.authenticationFacade).getLoggedTenant(utmAcquisitionData);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.status").value("FORBIDDEN")
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideProfileParameters")
        void parameterizedTests(ControllerParameter<ProfileTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = get("/api/tenant/profile")
                    .contentType("application/json");

            if (parameter.getParameterData().params != null) {
                mockMvcRequestBuilder.params(parameter.getParameterData().params);
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }


    }

    @Nested
    class deleteCoTenantTest {

        record DeleteTestParam(Long id) {
        }

        static List<Arguments> provideDeleteCoTenantParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var tenant = Tenant.builder().id(1L).build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    new DeleteTestParam(1L),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 if current user email is not verified",
                            new ControllerParameter<>(
                                    new DeleteTestParam(1L),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new AccessDeniedException("User not verified")).when(self.authenticationFacade).getLoggedTenant();
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 with empty body",
                            new ControllerParameter<>(
                                    new DeleteTestParam(1L),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(self.userService.deleteCoTenant(tenant, 1L)).thenReturn(true);
                                        return v;
                                    },
                                    List.of(content().bytes(new byte[0]))

                            )
                    ),
                    Pair.of("Should respond 403 when coTenant is not found",
                            new ControllerParameter<>(
                                    new DeleteTestParam(1L),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(self.userService.deleteCoTenant(tenant, 1L)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()

                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteCoTenantParameters")
        void parameterizedTests(ControllerParameter<DeleteTestParam> parameter) throws Exception {

            var urlTemplate = "/api/tenant/deleteCoTenant/";
            if (parameter.getParameterData() != null) {
                urlTemplate += parameter.getParameterData().id;
            }

            var mockMvcRequestBuilder = delete(urlTemplate)
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }

    @Nested
    class SendFileByMailTest {

        record SendFileByMailParam(String email, String shareType, String title, String message) {
        }

        static List<Arguments> provideSendFileByMailParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var tenant = Tenant.builder().id(1L).build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE", "Test Title", "Test Message"),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when invalid email is passed",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test", "SHARE_TYPE", "Test Title", "Test Message"),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when email limit reached",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE", "Test Title", "Test Message"),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new MailSentLimitException())
                                                .when(self.tenantService)
                                                .sendFileByMail(any(Tenant.class), any(ShareFileByMailForm.class));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when file is sent successfully",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE", "Test Title", "Test Message"),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        return v;
                                    },
                                    List.of(content().bytes(new byte[0]))
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSendFileByMailParameters")
        void parameterizedTests(ControllerParameter<SendFileByMailParam> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/tenant/sendFileByMail")
                    .contentType("application/json");

            if (parameter.getParameterData() != null) {
                Gson gson = new Gson();
                mockMvcRequestBuilder.content(gson.toJson(parameter.getParameterData()));
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class ExpectedProcessingTimeTest {

        record ExpectedProcessingTimeParam(Long tenantId) {
        }

        static List<Arguments> provideExpectedProcessingTimeParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var tenantId = 1L;
            var expectedProcessingTime = LocalDateTime.now();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    new ExpectedProcessingTimeParam(tenantId),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 with expected processing time",
                            new ControllerParameter<>(
                                    new ExpectedProcessingTimeParam(tenantId),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess(any(), eq(tenantId))).thenReturn(true);
                                        when(self.processingCapacityService.getExpectedProcessingTime(tenantId)).thenReturn(expectedProcessingTime);
                                        return v;
                                    },
                                    List.of(
                                            // We do not test the exact date because the json serializer of spring boot does not serialize the date in the same way
                                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                                    )
                            )
                    ),
                    Pair.of("Should respond 200 with null when expected processing time is not available",
                            new ControllerParameter<>(
                                    new ExpectedProcessingTimeParam(tenantId),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess(any(), eq(tenantId))).thenReturn(true);
                                        when(self.processingCapacityService.getExpectedProcessingTime(tenantId)).thenReturn(null);
                                        return v;
                                    },
                                    List.of(
                                            content().bytes(new byte[0])
                                    )
                            )
                    ),
                    Pair.of("Should respond 403 when tenant has no permission on requested tenant",
                            new ControllerParameter<>(
                                    new ExpectedProcessingTimeParam(2L),
                                    403,
                                    jwt().jwt(jwt -> jwt.subject("keycloak-user-id")).authorities(new SimpleGrantedAuthority("SCOPE_dossier")),
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess("keycloak-user-id", 2L)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideExpectedProcessingTimeParameters")
        void parameterizedTests(ControllerParameter<ExpectedProcessingTimeParam> parameter) throws Exception {

            var urlTemplate = "/api/tenant/" + parameter.getParameterData().tenantId + "/expectedProcessingTime";

            var mockMvcRequestBuilder = get(urlTemplate)
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class DoNotArchiveTest {

        record DoNotArchiveParam(String token) {
        }

        static List<Arguments> provideDoNotArchiveParameters() {
            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 404 when token is invalid",
                            new ControllerParameter<>(
                                    new DoNotArchiveParam(null),
                                    404,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when token is valid",
                            new ControllerParameter<>(
                                    new DoNotArchiveParam("token"),
                                    200,
                                    null,
                                    (v) -> {
                                        doNothing().when(self.tenantService).doNotArchive("token");
                                        return v;
                                    },
                                    List.of(content().bytes(new byte[0]))
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDoNotArchiveParameters")
        void parameterizedTests(ControllerParameter<DoNotArchiveParam> parameter) throws Exception {

            var urlTemplate = "/api/tenant/doNotArchive/";
            if (parameter.getParameterData().token != null) {
                urlTemplate += parameter.getParameterData().token;
            }

            var mockMvcRequestBuilder = get(urlTemplate)
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

}
