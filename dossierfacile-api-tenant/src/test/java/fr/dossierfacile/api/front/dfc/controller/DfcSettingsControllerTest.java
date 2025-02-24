package fr.dossierfacile.api.front.dfc.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.PartnerSettingsMapperImpl;
import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(DfcSettingsController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, PartnerSettingsMapperImpl.class, ResourceServerConfig.class, PartnerSettings.class})
public class DfcSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private static ClientAuthenticationFacade clientAuthenticationFacade;

    @MockBean
    private static UserApiService userApiService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static ObjectMapper mapper = new ObjectMapper();

    @BeforeEach()
    void beforeEach() {
        reset(clientAuthenticationFacade, userApiService);
    }

    @Nested
    class GetSettingsTests {

        static List<Arguments> provideGetSettingsParameters() throws JsonProcessingException {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("client_id", "client_id");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDfc = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc")).jwt(jwt);
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithWrongScope = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier")).jwt(jwt);

            UserApi userApi = UserApi.builder()
                    .id(1L)
                    .name("test")
                    .version(1)
                    .email("test@test.fr")
                    .urlCallback("http://localhost")
                    .partnerApiKeyCallback("test")
                    .build();

            PartnerSettings partnerSettings = new PartnerSettings();
            partnerSettings.setEmail("test@test.fr");
            partnerSettings.setUrlCallback("http://localhost");
            partnerSettings.setPartnerApiKeyCallback("test");
            partnerSettings.setVersion(1);
            partnerSettings.setName("test");

            var expectedJson = mapper.writeValueAsString(partnerSettings);

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when wrong scope is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtWithWrongScope,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        return v;
                                    },
                                    List.of(
                                            content().json(expectedJson)
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetSettingsParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = get("/dfc/api/v1/settings")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class UpdateSettingsTests {

        record UpdateSettingsTestParameter(PartnerSettings settings) {
        }

        static List<Arguments> provideUpdateSettingsParameters() throws JsonProcessingException {

            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("client_id", "client_id");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDfc = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc")).jwt(jwt);
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithWrongScope = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier")).jwt(jwt);

            UserApi userApi = UserApi.builder()
                    .id(1L)
                    .name("test")
                    .version(1)
                    .email("test@test.fr")
                    .urlCallback("http://localhost")
                    .partnerApiKeyCallback("test")
                    .build();

            PartnerSettings partnerSettings = new PartnerSettings();
            partnerSettings.setName("test");
            partnerSettings.setVersion(2);
            partnerSettings.setEmail("test@test.fr");
            partnerSettings.setUrlCallback("http://localhost");
            partnerSettings.setPartnerApiKeyCallback("test2");

            var expectedUserApiModified = UserApi.builder()
                    .id(1L)
                    .name("test")
                    .version(2)
                    .email("test@test.fr")
                    .urlCallback("http://localhost")
                    .partnerApiKeyCallback("test2")
                    .build();

            var expectedJson = mapper.writeValueAsString(partnerSettings);

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    new UpdateSettingsTestParameter(partnerSettings),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when not the good scope",
                            new ControllerParameter<>(
                                    new UpdateSettingsTestParameter(partnerSettings),
                                    403,
                                    jwtWithWrongScope,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    new UpdateSettingsTestParameter(partnerSettings),
                                    200,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        when(userApiService.update(any(), any())).thenReturn(expectedUserApiModified);
                                        return v;
                                    },
                                    List.of(
                                            content().json(expectedJson)
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideUpdateSettingsParameters")
        void parameterizedTests(ControllerParameter<UpdateSettingsTestParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = patch("/dfc/api/v1/settings")
                    .contentType("application/json");

            if (parameter.parameterData.settings != null) {
                mockMvcRequestBuilder.content(mapper.writeValueAsString(parameter.parameterData.settings));
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}