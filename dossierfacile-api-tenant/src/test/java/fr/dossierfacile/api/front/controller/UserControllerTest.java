package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private static UserService userService;

    @MockBean
    private static AuthenticationFacade authenticationFacade;

    @Test
    void shouldReturnNotFoundForInvalidUrl() throws Exception {
        mockMvc.perform(
                get("/api/user/invalidUrl").with(jwt())
        ).andDo(print()).andExpect(status().is(404));
    }


    @Nested
    class ForgotPasswordTests {

        record ForgotPasswordTestParameter(String email) {
        }

        static List<Arguments> provideForgotPasswordParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new ForgotPasswordTestParameter("test@test.fr"),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when no email is passed",
                            new ControllerParameter<>(
                                    new ForgotPasswordTestParameter(null),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when invalid email is passed",
                            new ControllerParameter<>(
                                    new ForgotPasswordTestParameter("test"),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when valid email is passed but user is not found",
                            new ControllerParameter<>(
                                    new ForgotPasswordTestParameter("test@test.fr"),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new UserNotFoundException("test@test.fr")).when(userService).forgotPassword(any());
                                        return v;
                                    },
                                    List.of(content().bytes(new byte[0]))
                            )
                    ),
                    Pair.of("Should respond 200 when valid email is passed but user is not found",
                            new ControllerParameter<>(
                                    new ForgotPasswordTestParameter("test@test.fr"),
                                    200,
                                    jwtTokenWithDossier,
                                    null,
                                    List.of(content().bytes(new byte[0]))
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideForgotPasswordParameters")
        void parameterizedTests(ControllerParameter<ForgotPasswordTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/user/forgotPassword")
                    .contentType("application/json");

            if (parameter.parameterData.email != null) {
                mockMvcRequestBuilder.content("{\"email\": \"" + parameter.parameterData.email + "\"}");
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }

    @Nested
    class CreatePasswordTests {

        record CreatePasswordTestParameter(String password) {
        }

        static List<Arguments> provideCreatePasswordParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var validPassword = "azerty";
            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            TenantModel tenantModel = TenantModel.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordTestParameter("azerty"),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when no password is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordTestParameter(null),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when empty password is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordTestParameter(""),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when user is not verified",
                            new ControllerParameter<>(
                                    new CreatePasswordTestParameter(validPassword),
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
                    Pair.of("Should respond 200 when user is verified",
                            new ControllerParameter<>(
                                    new CreatePasswordTestParameter(validPassword),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(userService.createPassword(tenant, validPassword)).thenReturn(tenantModel);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.id").value(1),
                                            jsonPath("$.email").value("test@test.fr")
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideCreatePasswordParameters")
        void parameterizedTests(ControllerParameter<CreatePasswordTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/user/createPassword")
                    .contentType("application/json");

            if (parameter.parameterData.password != null) {
                mockMvcRequestBuilder.content("{\"password\": \"" + parameter.parameterData.password + "\"}");
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }


    @Nested
    class CreatePasswordWithTokenTests {

        record CreatePasswordWithTokenParameter(String token, String password) {
        }

        static List<Arguments> provideCreatePasswordWithTokenParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var invalidToken = "invalid";
            var validToken = "test";
            var validPassword = "azerty";

            TenantModel tenantModel = TenantModel.builder().id(1L).email("test@test.fr").build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(null, null),
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when no token is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(null, null),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when no password is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(validToken, null),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when invalid password is passed",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(validToken, ""),
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when token is not found",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(invalidToken, validPassword),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new PasswordRecoveryTokenNotFoundException(invalidToken)).when(userService).createPassword(invalidToken, validPassword);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 and tenant model when token is founded",
                            new ControllerParameter<>(
                                    new CreatePasswordWithTokenParameter(validToken, validPassword),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(userService.createPassword(validToken, validPassword)).thenReturn(tenantModel);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.id").value(1),
                                            jsonPath("$.email").value(tenantModel.getEmail())
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideCreatePasswordWithTokenParameters")
        void parameterizedTests(ControllerParameter<CreatePasswordWithTokenParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/user/createPassword/" + parameter.parameterData.token)
                    .contentType("application/json");

            if (parameter.parameterData.password != null) {
                mockMvcRequestBuilder.content("{\"password\": \"" + parameter.parameterData.password + "\"}");
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }


    @Nested
    class DeleteAccountTests {

        static List<Arguments> provideDeleteAccountParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when not jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when user in not verified",
                            new ControllerParameter<>(
                                    null,
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
                    Pair.of("Should respond 403 when user in not verified",
                            new ControllerParameter<>(
                                    null,
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
                    Pair.of("Should respond 200 with empty result",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
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
        @MethodSource("provideDeleteAccountParameters")
        void parameterizedTest(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = delete("/api/user/deleteAccount")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }

    }


    @Nested
    class UnlinkFranceConnectTests {

        static List<Arguments> provideUnlinkFranceConnectParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when no jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when user is not verified",
                            new ControllerParameter<>(
                                    null,
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
                    Pair.of("Should respond 200 when user is verified",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
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
        @MethodSource("provideUnlinkFranceConnectParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = delete("/api/user/franceConnect")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }


    @Nested
    class LogoutTests {

        static List<Arguments> provideLogoutParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when no jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(authenticationFacade.getKeycloakUserId()).thenReturn("keycloakId");
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
        @MethodSource("provideLogoutParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/user/logout")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }


}
