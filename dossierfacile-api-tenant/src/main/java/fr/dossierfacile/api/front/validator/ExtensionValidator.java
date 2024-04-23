package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.annotation.Extension;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class ExtensionValidator implements ConstraintValidator<Extension, DocumentForm> {
    private static final List<String> contentTypes = Arrays.asList(
            "image/png",
            "image/jpeg",
            "application/pdf"
    );

    @Override
    public void initialize(Extension constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        return documentForm.getDocuments().stream().allMatch(f -> contentTypes.contains(f.getContentType()));
    }
}
