package fr.dossierfacile.common.validator;

import fr.dossierfacile.common.validator.annotation.SizeFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OWASP File Upload — "Set a file size limit".
 * <p>
 * Validates that each non-empty file in the list does not exceed the configured maximum size in megabytes.
 */
public class SizeFileValidator implements ConstraintValidator<SizeFile, List<MultipartFile>> {

    private double max;

    @Override
    public void initialize(SizeFile constraintAnnotation) {
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext context) {
        if (files == null) {
            return true;
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .allMatch(file -> (file.getSize() / (1024.0 * 1024.0)) <= max);
    }
}
