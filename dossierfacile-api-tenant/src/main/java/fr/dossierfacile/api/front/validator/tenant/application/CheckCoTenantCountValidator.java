package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CheckCoTenantCount;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collections;
import java.util.List;


public class CheckCoTenantCountValidator implements ConstraintValidator<CheckCoTenantCount, ApplicationFormV2> {

    @Override
    public void initialize(CheckCoTenantCount constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        // An explicit "coTenants": null in the request body overrides the field initializer
        List<CoTenantForm> coTenants = applicationForm.getCoTenants() == null
                ? Collections.emptyList()
                : applicationForm.getCoTenants();
        if (applicationForm.getApplicationType() == ApplicationType.COUPLE) {
            return coTenants.size() == 1;
        }
        if (applicationForm.getApplicationType() == ApplicationType.GROUP) {
            return !coTenants.isEmpty();
        }
        return coTenants.isEmpty();
    }
}
