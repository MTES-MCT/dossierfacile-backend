package fr.dossierfacile.api.front.validator.tenant.application.v2;

import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DistinctCoTenantFullNameList;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class DistinctCoTenantFullNameValidator implements ConstraintValidator<DistinctCoTenantFullNameList, List<CoTenantForm>> {

    @Override
    public void initialize(DistinctCoTenantFullNameList distinctEmailList) {
        //this method is empty
    }

    @Override
    public boolean isValid(List<CoTenantForm> coTenantForms, ConstraintValidatorContext constraintValidatorContext) {
        return coTenantForms
                .stream().map(t -> t.getFirstName() + t.getLastName()).distinct().count() == coTenantForms.size();
    }

}
