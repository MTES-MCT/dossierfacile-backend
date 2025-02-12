package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.SneakyThrows;
import org.javatuples.Pair;
import org.junit.jupiter.api.Named;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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

    static class ArgumentBuilder {

        public static <T> Arguments buildArguments(String name, T object) {
            return Arguments.of(Named.of(name, object));
        }

        @SafeVarargs
        public static <T> List<Arguments> buildListOfArguments(Pair<String, T>... pairList) {
            return Arrays.stream(pairList).map(pair -> buildArguments(pair.getValue0(), pair.getValue1())).toList();
        }
    }


    @Nested
    class ForgotPasswordTests {

        record ForgotPasswordTestParameter(String email, int status, RequestPostProcessor jwt,
                                           Function<Void, Void> setupMock, List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideForgotPasswordParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when not jwt is passed",
                            new ForgotPasswordTestParameter("test@test.fr",
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when no email is passed",
                            new ForgotPasswordTestParameter(
                                    null,
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when invalid email is passed",
                            new ForgotPasswordTestParameter(
                                    "test",
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 404 when valid email is passed but user is not found",
                            new ForgotPasswordTestParameter(
                                    "test@test.fr",
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new UserNotFoundException("test@test.fr")).when(userService).forgotPassword(any());
                                        return v;
                                    },
                                    List.of(content().bytes(new byte[0]))
                            )
                    )
            );
        }

        @SneakyThrows
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideForgotPasswordParameters")
        void parameterizedTests(ForgotPasswordTestParameter parameter) throws Exception {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = post("/api/user/forgotPassword")
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            if (parameter.email != null) {
                mockMvcConfiguration.content("{\"email\": \"" + parameter.email + "\"}");
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }

    }

    @Nested
    class CreatePasswordTests {

        record CreatePasswordTestParameter(String password, int status, RequestPostProcessor jwt,
                                           Function<Void, Void> setupMock, List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideCreatePasswordParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var validPassword = "azerty";
            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            TenantModel tenantModel = new TenantModel();
            tenantModel.setId(1L);
            tenantModel.setEmail("test@test.fr");

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when not jwt is passed",
                            new CreatePasswordTestParameter("azerty",
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when no password is passed",
                            new CreatePasswordTestParameter(
                                    null,
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when empty password is passed",
                            new CreatePasswordTestParameter(
                                    "",
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 403 when user is not verified",
                            new CreatePasswordTestParameter(
                                    validPassword,
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
                    new Pair<>("Should respond 200 when user is verified",
                            new CreatePasswordTestParameter(
                                    validPassword,
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
        void parameterizedTests(CreatePasswordTestParameter parameter) throws Exception {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = post("/api/user/createPassword")
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            if (parameter.password != null) {
                mockMvcConfiguration.content("{\"password\": \"" + parameter.password + "\"}");
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }

    }

    @Nested
    class CreatePasswordWithTokenTests {

        record CreatePasswordWithTokenParameter(String token, String password, int status, RequestPostProcessor jwt,
                                                Function<Void, Void> setupMock, List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideCreatePasswordWithTokenParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            var invalidToken = "invalid";
            var validToken = "test";
            var validPassword = "azerty";
            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            TenantModel tenantModel = new TenantModel();
            tenantModel.setId(1L);
            tenantModel.setEmail("test@test.fr");

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when not jwt is passed",
                            new CreatePasswordWithTokenParameter(
                                    null,
                                    null,
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when no token is passed",
                            new CreatePasswordWithTokenParameter(
                                    null,
                                    null,
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when no password is passed",
                            new CreatePasswordWithTokenParameter(
                                    validToken,
                                    null,
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 400 when invalid password is passed",
                            new CreatePasswordWithTokenParameter(
                                    validToken,
                                    "",
                                    400,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 404 when token is not found",
                            new CreatePasswordWithTokenParameter(
                                    invalidToken,
                                    validPassword,
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        doThrow(new PasswordRecoveryTokenNotFoundException(invalidToken)).when(userService).createPassword(invalidToken, validPassword);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 200 and tenant model when token is founded",
                            new CreatePasswordWithTokenParameter(
                                    validToken,
                                    validPassword,
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

        @SneakyThrows
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideCreatePasswordWithTokenParameters")
        void parameterizedTests(CreatePasswordWithTokenParameter parameter) {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = post("/api/user/createPassword/" + parameter.token)
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            if (parameter.password != null) {
                mockMvcConfiguration.content("{\"password\": \"" + parameter.password + "\"}");
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }

    }

    @Nested
    class DeleteAccountTests {

        record DeleteAccountParameter(int status, RequestPostProcessor jwt, Function<Void, Void> setupMock,
                                      List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideDeleteAccountParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            TenantModel tenantModel = new TenantModel();
            tenantModel.setId(1L);
            tenantModel.setEmail("test@test.fr");

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when not jwt is passed",
                            new DeleteAccountParameter(
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 403 when user in not verified",
                            new DeleteAccountParameter(
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
                    new Pair<>("Should respond 403 when user in not verified",
                            new DeleteAccountParameter(
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
                    new Pair<>("Should respond 200 with empty result",
                            new DeleteAccountParameter(
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

        @SneakyThrows
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteAccountParameters")
        void parameterizedTest(DeleteAccountParameter parameter) {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = delete("/api/user/deleteAccount")
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }

    }

    @Nested
    class UnlinkFranceConnectTests {

        record UnlinkFranceConnectTestParameter(int status, RequestPostProcessor jwt, Function<Void, Void> setupMock, List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideUnlinkFranceConnectParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when no jwt is passed",
                            new UnlinkFranceConnectTestParameter(
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 403 when user is not verified",
                            new UnlinkFranceConnectTestParameter(
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
                    new Pair<>("Should respond 200 when user is verified",
                            new UnlinkFranceConnectTestParameter(
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

        @SneakyThrows
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideUnlinkFranceConnectParameters")
        void parameterizedTests(UnlinkFranceConnectTestParameter parameter) {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = delete("/api/user/franceConnect")
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }
    }

    @Nested
    class LogoutTests {

        record LogoutTestParameter(int status, RequestPostProcessor jwt, Function<Void, Void> setupMock, List<ResultMatcher> resultMatchers) {
        }

        static List<Arguments> provideLogoutParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    new Pair<>("Should respond 403 when no jwt is passed",
                            new LogoutTestParameter(
                                    403,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    new Pair<>("Should respond 200 when jwt is passed",
                            new LogoutTestParameter(
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

        @SneakyThrows
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideLogoutParameters")
        void parameterizedTests(LogoutTestParameter parameter) {
            if (parameter.setupMock != null) {
                parameter.setupMock.apply(null);
            }

            var mockMvcConfiguration = post("/api/user/logout")
                    .contentType("application/json");

            if (parameter.jwt != null) {
                mockMvcConfiguration.with(parameter.jwt);
            }

            mockMvc.perform(mockMvcConfiguration)
                    .andDo(print())
                    .andExpect(status().is(parameter.status))
                    .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
        }
    }


}
