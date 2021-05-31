package fr.dossierfacile.api.front.validator.tenant;

import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.NumberOfDocument;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentValidator implements ConstraintValidator<NumberOfDocument, List<MultipartFile>> {

    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;
    private int max;
    private int min;
    private DocumentCategory documentCategory;

    @Override
    public void initialize(NumberOfDocument constraintAnnotation) {
        max = constraintAnnotation.max();
        min = constraintAnnotation.min();
        documentCategory = constraintAnnotation.documentCategory();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        long countOld = fileRepository.countFileByDocumentCategoryTenant(documentCategory, tenant);
        long countNew = files
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return min <= countNew + countOld && countNew + countOld <= max;
    }
}
