package fr.dossierfacile.api.front.validator.guarantor.natural_person.residency;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.config.featureflipping.OtherResidencyToggle;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.residency.CustomTextResidencyGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static fr.dossierfacile.api.front.validator.tenant.residency.CustomTextResidencyValidator.rejectOtherResidencyCategory;
import static fr.dossierfacile.api.front.validator.tenant.residency.CustomTextResidencyValidator.validateCustomText;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_RESIDENCY;

@Slf4j
@Component
@AllArgsConstructor
public class CustomTextResidencyGuarantorNaturalPersonValidator implements ConstraintValidator<CustomTextResidencyGuarantorNaturalPerson, DocumentResidencyGuarantorNaturalPersonForm> {

    private final OtherResidencyToggle otherResidencyToggle;

    @Override
    public boolean isValid(DocumentResidencyGuarantorNaturalPersonForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isCustomTextPresent = StringUtils.isNotBlank(documentForm.getCustomText());
        DocumentSubCategory subCategory = documentForm.getTypeDocumentResidency();

        if (subCategory == OTHER_RESIDENCY && otherResidencyToggle.isNotActive()) {
            return rejectOtherResidencyCategory(constraintValidatorContext, otherResidencyToggle);
        }

        return validateCustomText(constraintValidatorContext, subCategory, isCustomTextPresent);
    }

}
