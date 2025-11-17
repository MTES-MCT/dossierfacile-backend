package fr.dossierfacile.api.front.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.exception.MailSentLimitException;
import fr.dossierfacile.api.front.exception.PropertyNotFoundException;
import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.mapper.PropertyOMapperImpl;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.VersionedCategoriesMapper;
import fr.dossierfacile.common.service.interfaces.ProcessingCapacityService;
import fr.dossierfacile.common.utils.LocalDateTimeTypeAdapter;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TenantController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, TenantMapperImpl.class, VersionedCategoriesMapper.class, PropertyOMapperImpl.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"application.api.version = 4", "dossierfacile.common.global.exception.handler=true"})
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private static TenantService tenantService;

    @MockBean
    private static ProcessingCapacityService processingCapacityService;

    @MockBean
    private static PropertyService propertyService;

    @MockBean
    private static AuthenticationFacade authenticationFacade;

    @MockBean
    private static UserService userService;

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
                            // Todo : investigate why this request return a 401 instead of a 403
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
                                        when(authenticationFacade.getLoggedTenant(emptyAcquisitionData)).thenReturn(tenantWithoutCampaign);
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
                                        when(authenticationFacade.getLoggedTenant(utmAcquisitionData)).thenReturn(tenantWithoutCampaign);
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
                                        doThrow(new AccessDeniedException("User not verified")).when(authenticationFacade).getLoggedTenant(utmAcquisitionData);
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
    class getInfoOfPropertyAndOwnerTest {

        record GetInfoOfPropertyAndOwnerParameter(String token) {
        }

        static List<Arguments> provideParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var property = Property.builder()
                    .id(1L)
                    .address("test")
                    .name("test")
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            // Todo : investigate why this request return a 401 instead of a 403
                            new ControllerParameter<>(
                                    null,
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when no token is passed",
                            new ControllerParameter<>(
                                    null,
                                    404,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when property is not found",
                            new ControllerParameter<>(
                                    new GetInfoOfPropertyAndOwnerParameter("token"),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new PropertyNotFoundException("token")).when(propertyService).getPropertyByToken("token");
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 with propertyInformations",
                            new ControllerParameter<>(
                                    new GetInfoOfPropertyAndOwnerParameter("token"),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(propertyService.getPropertyByToken("token")).thenReturn(property);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.id").value(1),
                                            jsonPath("$.address").value("test"),
                                            jsonPath("$.name").value("test")
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideParameters")
        void parameterizedTests(ControllerParameter<GetInfoOfPropertyAndOwnerParameter> parameter) throws Exception {

            var urlTemplate = "/api/tenant/property/";
            if (parameter.getParameterData() != null) {
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

    @Nested
    class deleteCoTenantTest {

        record DeleteTestParam(Long id) {
        }

        static List<Arguments> provideDeleteCoTentParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var tenant = Tenant.builder().id(1L).build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new DeleteTestParam(1L),
                                    403,
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
                                        doThrow(new AccessDeniedException("User not verified")).when(authenticationFacade).getLoggedTenant();
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
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(userService.deleteCoTenant(tenant, 1L)).thenReturn(true);
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
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(userService.deleteCoTenant(tenant, 1L)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()

                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteCoTentParameters")
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
    class linkFranceConnectTest {

        record LinkFranceConnectParam(String url) {
        }

        static List<Arguments> provideFranceConnectParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new LinkFranceConnectParam("test.com"),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 bad request when no url is passed",
                            new ControllerParameter<>(
                                    new LinkFranceConnectParam(null),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should return france connect link url",
                            new ControllerParameter<LinkFranceConnectParam>(
                                    new LinkFranceConnectParam("test.com"),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getFranceConnectLink("test.com")).thenReturn("test.com");
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$").value("test.com")
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideFranceConnectParameters")
        void parameterizedTests(ControllerParameter<LinkFranceConnectParam> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/tenant/linkFranceConnect")
                    .contentType("application/json");

            if (parameter.getParameterData().url != null) {
                mockMvcRequestBuilder.content("{\"url\":\"" + parameter.getParameterData().url + "\"}");
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }

    @Nested
    class SendFileByMailTest {

        record SendFileByMailParam(String email, String shareType) {
        }

        static List<Arguments> provideSendFileByMailParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var tenant = Tenant.builder().id(1L).build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE"),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when invalid email is passed",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test", "SHARE_TYPE"),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when email limit reached",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE"),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new MailSentLimitException()).when(tenantService).sendFileByMail(tenant, ShareFileByMailForm.builder().email("test@test.com").daysValid(30).build());
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when file is sent successfully",
                            new ControllerParameter<>(
                                    new SendFileByMailParam("test@test.com", "SHARE_TYPE"),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
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

            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();

            return ArgumentBuilder.buildListOfArguments(
                    // Todo : investigate why 401
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
                                        when(processingCapacityService.getExpectedProcessingTime(tenantId)).thenReturn(expectedProcessingTime);
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
                                        when(processingCapacityService.getExpectedProcessingTime(tenantId)).thenReturn(null);
                                        return v;
                                    },
                                    List.of(
                                            content().bytes(new byte[0])
                                    )
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
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    new DoNotArchiveParam("token"),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when token is invalid",
                            new ControllerParameter<>(
                                    new DoNotArchiveParam(null),
                                    404,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when token is valid",
                            new ControllerParameter<>(
                                    new DoNotArchiveParam("token"),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doNothing().when(tenantService).doNotArchive("token");
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
