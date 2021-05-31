package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctEmailList;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class DistinctEmailListValidator implements ConstraintValidator<DistinctEmailList, List<String>> {

    @Override
    public void initialize(DistinctEmailList distinctEmailList) {
        //this method is empty
    }

    @Override
    public boolean isValid(List<String> coTenantForms, ConstraintValidatorContext constraintValidatorContext) {
        return coTenantForms.stream().map(String::toLowerCase).distinct().count() == coTenantForms.size();
    }

}
