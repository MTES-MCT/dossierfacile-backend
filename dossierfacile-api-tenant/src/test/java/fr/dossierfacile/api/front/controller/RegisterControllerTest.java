package fr.dossierfacile.api.front.controller;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.PropertyOMapperImpl;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.NumberOfPagesValidator;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.mapper.VersionedCategoriesMapper;
import fr.dossierfacile.common.service.interfaces.LogService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(RegisterController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        TenantMapperImpl.class,
        PropertyOMapperImpl.class,
        VersionedCategoriesMapper.class,
        GlobalExceptionHandler.class,
        NumberOfPagesValidator.class,
        ResourceServerConfig.class
}
)
@TestPropertySource(properties = {"application.api.version = 4", "dossierfacile.common.global.exception.handler=true"})
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

}
