package fr.dossierfacile.api.front.validator.tenant.residency;

import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.residency.CustomTextResidency;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_RESIDENCY;

@Slf4j
@Component
@AllArgsConstructor
public class CustomTextResidencyValidator implements ConstraintValidator<CustomTextResidency, DocumentResidencyForm> {

    @Override
    public boolean isValid(DocumentResidencyForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isCustomTextPresent = StringUtils.isNotBlank(documentForm.getCustomText());
        DocumentSubCategory subCategory = documentForm.getTypeDocumentResidency();

        return validateCustomText(constraintValidatorContext, subCategory, isCustomTextPresent);
    }

    public static boolean validateCustomText(ConstraintValidatorContext constraintValidatorContext, DocumentSubCategory subCategory, boolean isCustomTextPresent) {
        if (subCategory == OTHER_RESIDENCY && !isCustomTextPresent) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{jakarta.validation.constraints.NotBlank.message}")
                    .addPropertyNode("customText").addConstraintViolation();
            log.info("Rejecting {} update because customText should not be blank", subCategory);
            return false;
        }

        if (subCategory != OTHER_RESIDENCY && isCustomTextPresent) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{jakarta.validation.constraints.Null.message}")
                    .addPropertyNode("customText").addConstraintViolation();
            log.info("Rejecting {} update because customText should be null", subCategory);
            return false;
        }

        return true;
    }

}
