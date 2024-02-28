package fr.dossierfacile.api.front.validator.guarantor.natural_person.residency;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.residency.CustomTextResidencyGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static fr.dossierfacile.api.front.validator.tenant.residency.CustomTextResidencyValidator.validateCustomText;

@Slf4j
@Component
@AllArgsConstructor
public class CustomTextResidencyGuarantorNaturalPersonValidator implements ConstraintValidator<CustomTextResidencyGuarantorNaturalPerson, DocumentResidencyGuarantorNaturalPersonForm> {

    @Override
    public boolean isValid(DocumentResidencyGuarantorNaturalPersonForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isCustomTextPresent = StringUtils.isNotBlank(documentForm.getCustomText());
        DocumentSubCategory subCategory = documentForm.getTypeDocumentResidency();

        return validateCustomText(constraintValidatorContext, subCategory, isCustomTextPresent);
    }

}
