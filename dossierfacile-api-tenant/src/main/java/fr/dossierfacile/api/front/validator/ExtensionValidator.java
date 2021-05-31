package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.validator.anotation.Extension;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class ExtensionValidator implements ConstraintValidator<Extension, MultipartFile> {
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
    public boolean isValid(MultipartFile file, ConstraintValidatorContext constraintValidatorContext) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        return contentTypes.contains(file.getContentType());
    }
}
