package fr.dossierfacile.api.front.validator.tenant.application.v2;


import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DeniedJoinTenant;
import fr.dossierfacile.common.enums.TenantType;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeniedJoinTenantValidator extends TenantConstraintValidator<DeniedJoinTenant, ApplicationFormV2> {

    @Override
    public void initialize(DeniedJoinTenant constraintAnnotation) {
        //this method is empty.
    }
    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(applicationForm);
        if (tenant == null) {
            return true;
        }
        return tenant.getTenantType() != TenantType.JOIN;
    }
}
