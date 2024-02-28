package fr.dossierfacile.api.front.validator.tenant.application.v2;

import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DistinctCoTenantEmailList;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class DistinctCoTenantEmailListValidator implements ConstraintValidator<DistinctCoTenantEmailList, List<CoTenantForm>> {

    @Override
    public void initialize(DistinctCoTenantEmailList distinctEmailList) {
        //this method is empty
    }

    @Override
    public boolean isValid(List<CoTenantForm> coTenantForms, ConstraintValidatorContext constraintValidatorContext) {
        var emails = coTenantForms.stream()
                .filter(t -> StringUtils.isNotBlank(t.getEmail())).collect(Collectors.toList());
        return emails.stream().map(CoTenantForm::getEmail).distinct().count() == emails.size();
    }

}
