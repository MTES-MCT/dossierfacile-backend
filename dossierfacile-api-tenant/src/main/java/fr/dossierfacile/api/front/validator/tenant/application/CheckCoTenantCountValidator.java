package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CheckCoTenantCount;
import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class CheckCoTenantCountValidator implements ConstraintValidator<CheckCoTenantCount, ApplicationFormV2> {

    @Override
    public void initialize(CheckCoTenantCount constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        if (applicationForm.getApplicationType() == ApplicationType.COUPLE) {
            return applicationForm.getCoTenants().size() == 1;
        }
        if (applicationForm.getApplicationType() == ApplicationType.GROUP) {
            return !applicationForm.getCoTenants().isEmpty();
        }
        return applicationForm.getCoTenants().isEmpty();
    }
}
