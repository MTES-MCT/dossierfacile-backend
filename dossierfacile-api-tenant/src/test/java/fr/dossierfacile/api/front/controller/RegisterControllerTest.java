package fr.dossierfacile.api.front.controller;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.validator.tenant.application.ApplicationRegistrationValidator;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.NumberOfPagesValidator;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.service.interfaces.LogService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegisterController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        TenantMapperImpl.class,
        GlobalExceptionHandler.class,
        NumberOfPagesValidator.class,
        ResourceServerConfig.class
}
)
@TestPropertySource(properties = {"dossierfacile.common.global.exception.handler=true"})
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private FileRepository fileRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private FileUploadPreprocessor fileUploadPreprocessor;

    @MockitoBean
    private ApplicationRegistrationValidator applicationRegistrationValidator;

    // Référence statique à l’instance courante du test
    private static RegisterControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @Nested
    class NamesFinancialTest {
        private record NamesFinancialParameter(NamesForm namesForm) {
        }

        static List<Arguments> provideNamesParameter() {
            var apartmentSharing = ApartmentSharing.builder().id(1L).build();
            var actualTenant = Tenant.builder().id(1L).franceConnect(false).apartmentSharing(apartmentSharing).build();

            apartmentSharing.setTenants(List.of(actualTenant));
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when not jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when bad request",
                            new ControllerParameter<>(
                                    new NamesFinancialParameter(new NamesForm(
                                            null,
                                            "firstName",
                                            "lastName",
                                            null,
                                            null,
                                            false,
                                            TenantOwnerType.SELF
                                    )),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(actualTenant);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200",
                            new ControllerParameter<>(
                                    new NamesFinancialParameter(new NamesForm(
                                            1L,
                                            "firstName",
                                            "lastName",
                                            null,
                                            null,
                                            false,
                                            TenantOwnerType.SELF
                                    )),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {

                                        actualTenant.setFirstName("firstName");
                                        actualTenant.setLastName("lastName");

                                        TenantModel tenantModel = TenantModel.builder()
                                                .id(1L)
                                                .firstName("firstName")
                                                .lastName("lastName")
                                                .franceConnect(false)
                                                .build();
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(actualTenant);
                                        when(self.tenantService.findById(any())).thenReturn(actualTenant);
                                        when(self.tenantService.saveStepRegister(any(), any(), any())).thenReturn(
                                                tenantModel
                                        );
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideNamesParameter")
        void parameterizedTests(ControllerParameter<NamesFinancialParameter> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/api/register/names").contentType("application/json");

            if (parameter.getParameterData() != null) {
                Gson gson = new Gson();
                mockMvcRequestBuilder.content(gson.toJson(parameter.getParameterData().namesForm));
            }

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class ApplicationV2Test {

        private final Gson gson = new Gson();
        private final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtToken =
                jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

        @Test
        void shouldReturn200WhenApplicationIsValid() throws Exception {
            var apartmentSharing = ApartmentSharing.builder().id(1L).build();
            var tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).build();
            apartmentSharing.setTenants(List.of(tenant));

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                    .build();

            when(authenticationFacade.getTenant(null)).thenReturn(tenant);
            when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
            when(applicationRegistrationValidator.hasValidStructure(form)).thenReturn(true);
            when(applicationRegistrationValidator.validate(tenant, form)).thenReturn(Optional.empty());
            when(tenantService.saveStepRegister(any(), any(), any())).thenReturn(TenantModel.builder().id(1L).build());

            mockMvc.perform(post("/api/register/application/v2")
                            .contentType("application/json")
                            .content(gson.toJson(form))
                            .with(jwtToken))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturn400WithCodeWhenEmailIsMissing() throws Exception {
            var apartmentSharing = ApartmentSharing.builder().id(1L).build();
            var tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).build();

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, null)))
                    .build();

            when(authenticationFacade.getTenant(null)).thenReturn(tenant);
            when(applicationRegistrationValidator.hasValidStructure(form)).thenReturn(true);
            when(applicationRegistrationValidator.validate(tenant, form))
                    .thenReturn(Optional.of(ApplicationErrorCode.CO_TENANT_EMAIL_REQUIRED));

            mockMvc.perform(post("/api/register/application/v2")
                            .contentType("application/json")
                            .content(gson.toJson(form))
                            .with(jwtToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CO_TENANT_EMAIL_REQUIRED"));
        }

        @Test
        void shouldReturn409WithCodeWhenEmailAlreadyExists() throws Exception {
            var apartmentSharing = ApartmentSharing.builder().id(1L).build();
            var tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).build();

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "taken@example.com")))
                    .build();

            when(authenticationFacade.getTenant(null)).thenReturn(tenant);
            when(applicationRegistrationValidator.hasValidStructure(form)).thenReturn(true);
            when(applicationRegistrationValidator.validate(tenant, form))
                    .thenReturn(Optional.of(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS));

            mockMvc.perform(post("/api/register/application/v2")
                            .contentType("application/json")
                            .content(gson.toJson(form))
                            .with(jwtToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CO_TENANT_EMAIL_ALREADY_EXISTS"));
        }
    }

}
