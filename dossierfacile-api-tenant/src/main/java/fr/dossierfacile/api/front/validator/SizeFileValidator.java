package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SizeFileValidator implements ConstraintValidator<SizeFile, List<MultipartFile>> {

    private double max;

    private TypeDocumentValidation typeDocumentValidation;

    @Override
    public void initialize(SizeFile constraintAnnotation) {
        max = constraintAnnotation.max();
        typeDocumentValidation = constraintAnnotation.typeDocumentValidation();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext constraintValidatorContext) {
        if (typeDocumentValidation == TypeDocumentValidation.ALL_FILES) {
            ByteArrayOutputStream byteArrayOutputStream = Utility.mergeMultipartFiles(files.stream().filter(f -> !f.isEmpty()).collect(Collectors.toList()));
            double length = byteArrayOutputStream.size() / (1024.0 * 1024.0);
            return length <= max;
        } else {
            return files.stream().filter(f -> !f.isEmpty()).allMatch(f -> (f.getSize() / (1024.0 * 1024.0) <= max));
        }
    }
}
