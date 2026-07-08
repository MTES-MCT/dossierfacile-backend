package fr.dossierfacile.api.front.controller;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.NumberOfPagesValidator;
import fr.dossierfacile.api.front.validator.tenant.application.ApplicationRegistrationValidator;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.Test;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class ApplicationV2ControllerTest {

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

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(applicationRegistrationValidator.hasValidStructure(any())).thenReturn(true);
        when(applicationRegistrationValidator.validate(any(), any())).thenReturn(Optional.empty());
        when(tenantService.saveStepRegister(any(), any(), any())).thenReturn(TenantModel.builder().id(1L).build());

        mockMvc.perform(post("/api/register/application/v2")
                        .contentType("application/json")
                        .content(gson.toJson(form))
                        .with(jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenStructureIsInvalid() throws Exception {
        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.ALONE)
                .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                .build();

        when(applicationRegistrationValidator.hasValidStructure(any())).thenReturn(false);

        mockMvc.perform(post("/api/register/application/v2")
                        .contentType("application/json")
                        .content(gson.toJson(form))
                        .with(jwtToken))
                .andExpect(status().isBadRequest());

        verify(tenantService, never()).saveStepRegister(any(), any(), any());
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

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(applicationRegistrationValidator.hasValidStructure(any())).thenReturn(true);
        when(applicationRegistrationValidator.validate(any(), any()))
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

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(applicationRegistrationValidator.hasValidStructure(any())).thenReturn(true);
        when(applicationRegistrationValidator.validate(any(), any()))
                .thenReturn(Optional.of(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post("/api/register/application/v2")
                        .contentType("application/json")
                        .content(gson.toJson(form))
                        .with(jwtToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CO_TENANT_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturn400WhenJoinTenantSubmitsWithOwnSession() throws Exception {
        var apartmentSharing = ApartmentSharing.builder().id(1L).build();
        var joinTenant = Tenant.builder()
                .id(2L)
                .tenantType(TenantType.JOIN)
                .apartmentSharing(apartmentSharing)
                .build();

        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.ALONE)
                .coTenants(Collections.emptyList())
                .build();

        when(authenticationFacade.getLoggedTenant()).thenReturn(joinTenant);
        when(applicationRegistrationValidator.hasValidStructure(any())).thenReturn(true);
        when(applicationRegistrationValidator.validate(any(), any()))
                .thenReturn(Optional.of(ApplicationErrorCode.APPLICATION_TYPE_DENIED_FOR_JOIN));

        mockMvc.perform(post("/api/register/application/v2")
                        .contentType("application/json")
                        .content(gson.toJson(form))
                        .with(jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APPLICATION_TYPE_DENIED_FOR_JOIN"));

        verify(tenantService, never()).saveStepRegister(any(), any(), any());
    }
}
