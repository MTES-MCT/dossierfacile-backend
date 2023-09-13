package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SizeFileValidator implements ConstraintValidator<SizeFile, List<MultipartFile>> {

    private double max;

    @Override
    public void initialize(SizeFile constraintAnnotation) {
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext constraintValidatorContext) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .allMatch(file -> (file.getSize() / (1024.0 * 1024.0) <= max));
    }
}
