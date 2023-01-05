package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeAcceptAccess;
import fr.dossierfacile.common.enums.ApplicationType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/** deprecated  since 202209 */
@Deprecated
public class CheckTenantTypeAcceptAccessValidator implements ConstraintValidator<CheckTenantTypeAcceptAccess, ApplicationForm> {
    @Override
    public void initialize(CheckTenantTypeAcceptAccess constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationForm applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = (applicationForm.getApplicationType() == ApplicationType.ALONE)
                || ((applicationForm.getApplicationType() == ApplicationType.GROUP
                || applicationForm.getApplicationType() == ApplicationType.COUPLE)
                && applicationForm.getAcceptAccess() != null
                && applicationForm.getAcceptAccess());

        if (!isValid) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{javax.validation.constraints.AssertTrue.message}")
                    .addPropertyNode("acceptAccess").addConstraintViolation();
        }

        return isValid;
    }
}
