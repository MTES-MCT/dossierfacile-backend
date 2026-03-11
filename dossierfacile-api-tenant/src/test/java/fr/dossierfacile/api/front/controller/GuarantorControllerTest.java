package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(GuarantorController.class)
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
class GuarantorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuarantorService guarantorService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static GuarantorControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    private static final Long GUARANTOR_ID = 1L;

    @Nested
    class DeleteGuarantorTest {

        record DeleteGuarantorParam() {}

        static List<Arguments> provideDeleteGuarantorParameters() {
            var jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            ApartmentSharing sharingAlone = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
            ApartmentSharing sharingCouple = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.COUPLE).build();
            ApartmentSharing sharingGroup = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.GROUP).build();

            Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharingAlone).build();
            sharingAlone.setTenants(List.of(tenant1));

            Tenant tenantCouple1 = Tenant.builder().id(1L).apartmentSharing(sharingCouple).build();
            Tenant tenantCouple2 = Tenant.builder().id(2L).apartmentSharing(sharingCouple).build();
            sharingCouple.setTenants(List.of(tenantCouple1, tenantCouple2));

            Tenant tenantGroup1 = Tenant.builder().id(1L).apartmentSharing(sharingGroup).build();
            Tenant tenantGroup2 = Tenant.builder().id(2L).apartmentSharing(sharingGroup).build();
            sharingGroup.setTenants(List.of(tenantGroup1, tenantGroup2));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 200 when tenant deletes own guarantor",
                            new ControllerParameter<>(
                                    new DeleteGuarantorParam(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant1);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when COUPLE tenant deletes co-tenant guarantor",
                            new ControllerParameter<>(
                                    new DeleteGuarantorParam(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenantCouple1);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when GROUP tenant tries to delete co-tenant guarantor",
                            new ControllerParameter<>(
                                    new DeleteGuarantorParam(),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenantGroup1);
                                        doThrow(new AccessDeniedException("Not authorized"))
                                                .when(self.guarantorService).delete(GUARANTOR_ID, tenantGroup1);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 401 when no JWT is provided",
                            new ControllerParameter<>(
                                    new DeleteGuarantorParam(),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when guarantor not found",
                            new ControllerParameter<>(
                                    new DeleteGuarantorParam(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant1);
                                        doThrow(new GuarantorNotFoundException(GUARANTOR_ID))
                                                .when(self.guarantorService).delete(GUARANTOR_ID, tenant1);
                                        Guarantor guarantor = Guarantor.builder().id(GUARANTOR_ID)
                                                .tenant(Tenant.builder().id(99L).build()).build();
                                        when(self.guarantorService.findById(GUARANTOR_ID)).thenReturn(guarantor);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteGuarantorParameters")
        void parameterizedTests(ControllerParameter<DeleteGuarantorParam> parameter) throws Exception {
            if (parameter.getRequestPostProcessor() == null) {
                mockMvc.perform(delete("/api/guarantor/{id}", GUARANTOR_ID))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(parameter.getStatus()));
            } else {
                ParameterizedTestHelper.runControllerTest(
                        mockMvc,
                        delete("/api/guarantor/{id}", GUARANTOR_ID).with(parameter.getRequestPostProcessor()),
                        parameter
                );
            }
        }
    }
}
