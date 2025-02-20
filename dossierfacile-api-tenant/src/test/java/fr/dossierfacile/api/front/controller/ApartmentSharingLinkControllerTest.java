package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ApartmentSharingLinkController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class})
public class ApartmentSharingLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private static AuthenticationFacade authenticationFacade;

    @MockBean
    private static ApartmentSharingLinkService apartmentSharingLinkService;

    @MockBean
    private static TenantService tenantService;

    @Nested
    class GetApartmentSharingLinksTests {

        record GetApartmentSharingLinksTestParameter() {
        }

        static List<Arguments> provideGetApartmentSharingLinksParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            List<ApartmentSharingLinkModel> links = Collections.emptyList();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    new GetApartmentSharingLinksTestParameter(),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new GetApartmentSharingLinksTestParameter(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(Tenant.builder().apartmentSharing(apartmentSharing).build());
                                        when(apartmentSharingLinkService.getLinksByMail(apartmentSharing)).thenReturn(links);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.links").isArray()
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetApartmentSharingLinksParameters")
        void parameterizedTests(ControllerParameter<GetApartmentSharingLinksTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = get("/api/application/links")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class UpdateApartmentSharingLinksStatusTests {

        record UpdateApartmentSharingLinksStatusTestParameter(Long id, Boolean enabled) {
        }

        static List<Arguments> provideUpdateApartmentSharingLinksStatusParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));


            ApartmentSharing apartmentSharing = new ApartmentSharing();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when no jwt is passed",
                            new ControllerParameter<>(
                                    new UpdateApartmentSharingLinksStatusTestParameter(1L, true),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when email is not verified",
                            new ControllerParameter<>(
                                    new UpdateApartmentSharingLinksStatusTestParameter(1L, true),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new AccessDeniedException("User not verified")).when(authenticationFacade).getLoggedTenant();
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.status").value("FORBIDDEN")
                                    )
                            )
                    ),
                    Pair.of("Should respond 404 when link not found",
                            new ControllerParameter<>(
                                    new UpdateApartmentSharingLinksStatusTestParameter(1L, true),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(Tenant.builder().apartmentSharing(apartmentSharing).build());
                                        doThrow(new NotFoundException()).when(apartmentSharingLinkService).updateStatus(1L, true, apartmentSharing);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 parameter enabled is missing",
                            new ControllerParameter<>(
                                    new UpdateApartmentSharingLinksStatusTestParameter(1L, null),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new UpdateApartmentSharingLinksStatusTestParameter(1L, true),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(Tenant.builder().apartmentSharing(apartmentSharing).build());
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideUpdateApartmentSharingLinksStatusParameters")
        void parameterizedTests(ControllerParameter<UpdateApartmentSharingLinksStatusTestParameter> parameter) throws Exception {

            var url = "/api/application/links";
            if (parameter.parameterData.id != null) {
                url += "/" + parameter.parameterData.id;
            }
            var mockMvcRequestBuilder = put(url)
                    .contentType("application/json");

            if (parameter.parameterData.enabled != null) {
                mockMvcRequestBuilder = mockMvcRequestBuilder.param("enabled", String.valueOf(parameter.parameterData.enabled));
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class ResendApartmentSharingLinkTests {

        record ResendApartmentSharingLinkTestParameter() {
        }

        static List<Arguments> provideResendApartmentSharingLinkParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = new Tenant();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when no jwt is passed",
                            new ControllerParameter<>(
                                    new ResendApartmentSharingLinkTestParameter(),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("should respond 403 when email is not verified",
                            new ControllerParameter<>(
                                    new ResendApartmentSharingLinkTestParameter(),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new AccessDeniedException("User not verified")).when(authenticationFacade).getLoggedTenant();
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.status").value("FORBIDDEN")
                                    )
                            )
                    ),
                    Pair.of("should respond 403 when accessing other apartmentSharing",
                            new ControllerParameter<>(
                                    new ResendApartmentSharingLinkTestParameter(),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new AccessDeniedException("Access Denied")).when(tenantService).resendLink(1L, tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("should respond 500 when link is disabled",
                        new ControllerParameter<>(
                            new ResendApartmentSharingLinkTestParameter(),
                            500,
                            jwtTokenWithDossier,
                            (v) -> {
                                when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                doThrow(new IllegalStateException("A disabled link cannot be sent")).when(tenantService).resendLink(1L, tenant);
                                return v;
                            },
                            Collections.emptyList()
                        )
                    ),
                    Pair.of("Should respond 500 when delay between 2 resend is too short",
                        new ControllerParameter<>(
                            new ResendApartmentSharingLinkTestParameter(),
                            500,
                            jwtTokenWithDossier,
                            (v) -> {
                                when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                doThrow(new IllegalStateException("Delay between two resend is too short")).when(tenantService).resendLink(1L, tenant);
                                return v;
                            },
                            Collections.emptyList()
                        )
                    ),
                    Pair.of("Should respond 500 when error while sending mail",
                            new ControllerParameter<>(
                                    new ResendApartmentSharingLinkTestParameter(),
                                    500,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        doThrow(new InternalError("Mail cannot be send - try later")).when(tenantService).resendLink(1L, tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new ResendApartmentSharingLinkTestParameter(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideResendApartmentSharingLinkParameters")
        void parameterizedTests(ControllerParameter<ResendApartmentSharingLinkTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/application/links/1/resend")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class DeleteApartmentSharingLinkTests {

        record DeleteApartmentSharingLinkTestParameter() {
        }

        static List<Arguments> provideDeleteApartmentSharingLinkParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = new Tenant();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when no jwt is passed",
                            new ControllerParameter<>(
                                    new DeleteApartmentSharingLinkTestParameter(),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when email is not verified",
                            new ControllerParameter<>(
                                    new DeleteApartmentSharingLinkTestParameter(),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new AccessDeniedException("User not verified")).when(authenticationFacade).getLoggedTenant();
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.status").value("FORBIDDEN")
                                    )
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new DeleteApartmentSharingLinkTestParameter(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteApartmentSharingLinkParameters")
        void parameterizedTests(ControllerParameter<DeleteApartmentSharingLinkTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = delete("/api/application/links/1")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}