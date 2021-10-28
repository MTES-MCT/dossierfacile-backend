package fr.dossierfacile.api.front.validator.tenant.honor_declaration;


import fr.dossierfacile.api.front.register.form.tenant.HonorDeclarationForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.honor_declaration.CheckHonorDeclarationClarification;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@AllArgsConstructor
public class CheckHonorDeclarationClarificationValidator implements ConstraintValidator<CheckHonorDeclarationClarification, HonorDeclarationForm> {

    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(CheckHonorDeclarationClarification constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(HonorDeclarationForm honorDeclarationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = authenticationFacade.getTenant(honorDeclarationForm.getTenantId());
        boolean isValid = tenant.getTenantType().name().equals(TenantType.CREATE.name());
        if (!isValid && honorDeclarationForm.getClarification() == null) {
            return true;
        }
        return isValid;
    }
}
