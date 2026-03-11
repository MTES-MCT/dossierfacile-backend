package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.MethodSecurityConfig;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.MessageService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.Tenant;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(MessageController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        ResourceServerConfig.class,
        MethodSecurityConfig.class,
        CustomRestExceptionHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "dossierfacile.common.global.exception.handler=true",
        "resource.server.config.csp=default-src 'self'"
})
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static MessageControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @Nested
    class GetAllMessagesTest {

        record GetAllMessagesParam(Long tenantId) {}

        static List<Arguments> provideGetAllMessagesParameters() {
            var jwtTokenWithDossier = jwt().jwt(jwt -> jwt.subject("keycloak-user-id"))
                    .authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            Tenant tenant = Tenant.builder().id(1L).build();
            List<MessageModel> messages = Collections.emptyList();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no JWT is passed",
                            new ControllerParameter<>(
                                    new GetAllMessagesParam(1L),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when tenant has no permission on requested tenantId",
                            new ControllerParameter<>(
                                    new GetAllMessagesParam(99L),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess("keycloak-user-id", 99L)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when tenant has permission",
                            new ControllerParameter<>(
                                    new GetAllMessagesParam(1L),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess("keycloak-user-id", 1L)).thenReturn(true);
                                        when(self.authenticationFacade.getTenant(1L)).thenReturn(tenant);
                                        when(self.messageService.findAll(tenant)).thenReturn(messages);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetAllMessagesParameters")
        void parameterizedTests(ControllerParameter<GetAllMessagesParam> parameter) throws Exception {
            var mockMvcRequestBuilder = get("/api/message")
                    .contentType("application/json");
            if (parameter.getParameterData().tenantId() != null) {
                mockMvcRequestBuilder.param("tenantId", parameter.getParameterData().tenantId().toString());
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}
