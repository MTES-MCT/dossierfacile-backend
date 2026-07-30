package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoTenantsEmailRequiredValidatorTest {

    private final CoTenantsEmailRequiredValidator validator = new CoTenantsEmailRequiredValidator();

    private ApplicationFormV2 form(ApplicationType type, String email) {
        return ApplicationFormV2.builder()
                .applicationType(type)
                .acceptAccess(true)
                .coTenants(List.of(new CoTenantForm("test", "test", null, email)))
                .build();
    }

    @Test
    void couple_requires_cotenant_email() {
        assertThat(validator.isValid(form(ApplicationType.COUPLE, ""), null)).isFalse();
        assertThat(validator.isValid(form(ApplicationType.COUPLE, "spouse@test.fr"), null)).isTrue();
    }

    @Test
    void group_requires_cotenant_email() {
        assertThat(validator.isValid(form(ApplicationType.GROUP, ""), null)).isFalse();
        assertThat(validator.isValid(form(ApplicationType.GROUP, "flatmate@test.fr"), null)).isTrue();
    }

    @Test
    void alone_does_not_require_cotenant_email() {
        assertThat(validator.isValid(form(ApplicationType.ALONE, ""), null)).isTrue();
    }
}
