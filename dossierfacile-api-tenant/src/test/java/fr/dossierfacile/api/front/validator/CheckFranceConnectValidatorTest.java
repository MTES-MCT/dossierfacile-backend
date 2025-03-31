package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.security.AuthenticationFacadeImpl;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.TenantServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.tenant.name.CheckFranceConnectValidator;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantOwnerType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckFranceConnectValidatorTest {

    private final AuthenticationFacade authenticationFacade = mock(AuthenticationFacadeImpl.class);
    private final TenantService tenantService = mock(TenantServiceImpl.class);

    private final CheckFranceConnectValidator validator = new CheckFranceConnectValidator();

    record ValidatorTestParam(
            NamesForm namesForm,
            Tenant currentTenant,
            Boolean result
    ) {
    }

    static Stream<Arguments> provideArgumentsForTest() {
        return Stream.of(
                Arguments.of(new ValidatorTestParam(
                        new NamesForm(
                                1L,
                                "John",
                                "Doe",
                                null,
                                null,
                                false,
                                TenantOwnerType.SELF
                        ),
                        Tenant.builder()
                                .id(1L)
                                .firstName("test")
                                .lastName("test")
                                .franceConnect(false)
                                .build(),
                        true
                )),
                Arguments.of(new ValidatorTestParam(
                        new NamesForm(
                                1L,
                                "John",
                                "Doe",
                                null,
                                null,
                                false,
                                TenantOwnerType.SELF
                        ),
                        Tenant.builder()
                                .id(1L)
                                .firstName("test")
                                .lastName("test")
                                .franceConnect(true)
                                .build(),
                        false
                )),
                Arguments.of(new ValidatorTestParam(
                        new NamesForm(
                                1L,
                                "John",
                                "Doe",
                                null,
                                null,
                                false,
                                TenantOwnerType.THIRD_PARTY
                        ),
                        Tenant.builder()
                                .id(1L)
                                .firstName("test")
                                .lastName("test")
                                .franceConnect(true)
                                .build(),
                        true
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForTest")
    void validationTest(ValidatorTestParam validatorTestParam) {

        ReflectionTestUtils.setField(validator, "authenticationFacade", authenticationFacade);
        ReflectionTestUtils.setField(validator, "tenantService", tenantService);

        when(authenticationFacade.getLoggedTenant(any())).thenReturn(validatorTestParam.currentTenant);
        when(tenantService.findById(validatorTestParam.currentTenant.getId())).thenReturn(validatorTestParam.currentTenant);

        NamesForm namesForm = validatorTestParam.namesForm;

        boolean result = validator.isValid(namesForm, null);

        assertThat(result).isEqualTo(validatorTestParam.result);
    }

}
