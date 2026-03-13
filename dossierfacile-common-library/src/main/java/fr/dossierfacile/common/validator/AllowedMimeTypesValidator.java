package fr.dossierfacile.common.validator;

import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.Tika;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OWASP File Upload — "Validate the file type, don't trust the Content-Type header".
 * <p>
 * Uses Apache Tika to detect the real MIME type from file content (magic bytes),
 * not from the client-supplied Content-Type header which can be spoofed.
 */
public class AllowedMimeTypesValidator implements ConstraintValidator<AllowedMimeTypes, List<MultipartFile>> {

    private Set<String> allowedMimeTypes;
    private final Tika tika = new Tika();

    @Override
    public void initialize(AllowedMimeTypes constraintAnnotation) {
        this.allowedMimeTypes = Arrays.stream(constraintAnnotation.value())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext context) {
        if (files == null) {
            return true;
        }

        return files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .allMatch(this::isFileAllowed);
    }

    private boolean isFileAllowed(MultipartFile file) {
        try {
            // OWASP: Ensure input validation before validating extensions — use cleanPath to neutralize path traversal
            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
            String safePath = StringUtils.cleanPath(originalFilename);

            String detectedMimeType = tika.detect(file.getInputStream(), safePath);
            if (detectedMimeType == null) {
                return false;
            }

            return allowedMimeTypes.contains(detectedMimeType.toLowerCase());
        } catch (IOException e) {
            return false;
        }
    }
}
