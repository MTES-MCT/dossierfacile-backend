package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CoTenantsEmailRequiredForGroup;
import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@RequiredArgsConstructor
public class CoTenantsEmailRequiredForGroupValidator implements ConstraintValidator<CoTenantsEmailRequiredForGroup, ApplicationFormV2> {

    @Override
    public void initialize(CoTenantsEmailRequiredForGroup constraintAnnotation) {
        //this method is empty.
    }

    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        if (applicationForm.getApplicationType() != ApplicationType.GROUP || CollectionUtils.isEmpty(applicationForm.getCoTenants())) {
            return true;
        }
        return !applicationForm.getCoTenants().stream().anyMatch(t -> StringUtils.isBlank(t.getEmail()));
    }
}
