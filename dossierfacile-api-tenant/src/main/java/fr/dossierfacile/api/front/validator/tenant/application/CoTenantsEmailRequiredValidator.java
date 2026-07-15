package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CoTenantsEmailRequired;
import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@RequiredArgsConstructor
public class CoTenantsEmailRequiredValidator implements ConstraintValidator<CoTenantsEmailRequired, ApplicationFormV2> {

    @Override
    public void initialize(CoTenantsEmailRequired constraintAnnotation) {
        //this method is empty.
    }

    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        // Co-tenant email is required both for GROUP and COUPLE applications
        ApplicationType applicationType = applicationForm.getApplicationType();
        if ((applicationType != ApplicationType.GROUP && applicationType != ApplicationType.COUPLE)
                || CollectionUtils.isEmpty(applicationForm.getCoTenants())) {
            return true;
        }
        return applicationForm.getCoTenants().stream().noneMatch(t -> StringUtils.isBlank(t.getEmail()));
    }
}
