package fr.dossierfacile.api.front.validator.tenant;

import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.api.front.validator.anotation.tenant.NumberOfPages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class NumberOfPagesValidator implements ConstraintValidator<NumberOfPages, List<MultipartFile>> {

    private int max;

    @Override
    public void initialize(NumberOfPages constraintAnnotation) {
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext constraintValidatorContext) {
        ByteArrayOutputStream byteArrayOutputStream = Utility.mergeMultipartFiles(files.stream().filter(f -> !f.isEmpty()).collect(Collectors.toList()));
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        try (PDDocument pdDocument = PDDocument.load(inputStream)) {
            return pdDocument.getNumberOfPages() <= max;
        } catch (IOException e) {
            log.error("problem with validation of number of pages");
            return false;
        }
    }
}
