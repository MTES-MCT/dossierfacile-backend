package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.common.enums.TenantOwnerType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CheckBeneficiaryEmailValidatorTest {

    private final CheckBeneficiaryEmailValidator validator = new CheckBeneficiaryEmailValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, Answers.RETURNS_DEEP_STUBS);

    private NamesForm form(TenantOwnerType ownerType, String beneficiaryEmail) {
        NamesForm form = new NamesForm();
        form.setOwnerType(ownerType);
        form.setBeneficiaryEmail(beneficiaryEmail);
        return form;
    }

    @Test
    void should_reject_third_party_without_email() {
        assertThat(validator.isValid(form(TenantOwnerType.THIRD_PARTY, null), context)).isFalse();
        assertThat(validator.isValid(form(TenantOwnerType.THIRD_PARTY, " "), context)).isFalse();
    }

    @Test
    void should_accept_third_party_with_email() {
        assertThat(validator.isValid(form(TenantOwnerType.THIRD_PARTY, "beneficiary@dossierfacile.fr"), context)).isTrue();
    }

    @Test
    void should_accept_self_without_email() {
        assertThat(validator.isValid(form(TenantOwnerType.SELF, null), context)).isTrue();
    }

    @Test
    void should_accept_null_owner_type_without_email() {
        assertThat(validator.isValid(form(null, null), context)).isTrue();
    }
}
