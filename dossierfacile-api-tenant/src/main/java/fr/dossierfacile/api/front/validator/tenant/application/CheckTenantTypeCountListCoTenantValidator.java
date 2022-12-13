package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeCountListCoTenant;
import fr.dossierfacile.common.enums.ApplicationType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/** deprecated  since 202209 */
@Deprecated
public class CheckTenantTypeCountListCoTenantValidator implements ConstraintValidator<CheckTenantTypeCountListCoTenant, ApplicationForm> {

    @Override
    public void initialize(CheckTenantTypeCountListCoTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationForm applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        if (applicationForm.getApplicationType() == ApplicationType.COUPLE) {
            return applicationForm.getCoTenantEmail().size() == 1;
        }
        if (applicationForm.getApplicationType() == ApplicationType.GROUP) {
            return !applicationForm.getCoTenantEmail().isEmpty();
        }
        return applicationForm.getCoTenantEmail().isEmpty();
    }
}
