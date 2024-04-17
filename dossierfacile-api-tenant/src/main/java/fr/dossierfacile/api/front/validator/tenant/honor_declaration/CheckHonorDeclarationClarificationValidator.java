package fr.dossierfacile.api.front.validator.tenant.honor_declaration;


import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.honor_declaration.CheckHonorDeclarationClarification;
import fr.dossierfacile.common.enums.TenantType;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CheckHonorDeclarationClarificationValidator extends TenantConstraintValidator<CheckHonorDeclarationClarification, HonorDeclarationForm> {

    @Override
    public boolean isValid(HonorDeclarationForm honorDeclarationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(honorDeclarationForm);
        boolean isValid = tenant.getTenantType().name().equals(TenantType.CREATE.name());
        if (!isValid && honorDeclarationForm.getClarification() == null) {
            return true;
        }
        return isValid;
    }
}
