package fr.dossierfacile.api.front.validator.tenant.residency;

import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.config.featureflipping.OtherResidencyToggle;
import fr.dossierfacile.api.front.validator.anotation.tenant.residency.CustomTextResidency;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_RESIDENCY;

@Slf4j
@Component
@AllArgsConstructor
public class CustomTextResidencyValidator implements ConstraintValidator<CustomTextResidency, DocumentResidencyForm> {

    private final OtherResidencyToggle otherResidencyToggle;

    @Override
    public boolean isValid(DocumentResidencyForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isCustomTextPresent = StringUtils.isNotBlank(documentForm.getCustomText());
        DocumentSubCategory subCategory = documentForm.getTypeDocumentResidency();

        if (subCategory == OTHER_RESIDENCY && otherResidencyToggle.isNotActive()) {
            return rejectOtherResidencyCategory(constraintValidatorContext, otherResidencyToggle);
        }

        return validateCustomText(constraintValidatorContext, subCategory, isCustomTextPresent);
    }

    public static boolean rejectOtherResidencyCategory(ConstraintValidatorContext constraintValidatorContext, OtherResidencyToggle otherResidencyToggle) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext
                .buildConstraintViolationWithTemplate("OTHER_RESIDENCY will not be accepted until " + otherResidencyToggle.getActivationDate())
                .addConstraintViolation();
        log.info("Rejecting OTHER_RESIDENCY update because this category is not active yet");
        return false;
    }

    public static boolean validateCustomText(ConstraintValidatorContext constraintValidatorContext, DocumentSubCategory subCategory, boolean isCustomTextPresent) {
        if (subCategory == OTHER_RESIDENCY && !isCustomTextPresent) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{javax.validation.constraints.NotBlank.message}")
                    .addPropertyNode("customText").addConstraintViolation();
            log.info("Rejecting {} update because customText should not be blank", subCategory);
            return false;
        }

        if (subCategory != OTHER_RESIDENCY && isCustomTextPresent) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{javax.validation.constraints.Null.message}")
                    .addPropertyNode("customText").addConstraintViolation();
            log.info("Rejecting {} update because customText should be null", subCategory);
            return false;
        }

        return true;
    }

}
