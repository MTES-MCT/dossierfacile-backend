package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
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
public class NumberOfPagesValidator implements ConstraintValidator<NumberOfPages, DocumentForm> {

    private static final String PAGES = "documents";
    private static final String RESPONSE = "The number of new pages must be greater than 0";

    private final TenantService tenantService;
    private final FileRepository fileRepository;
    private DocumentCategory documentCategory;
    private int max;

    @Override
    public void initialize(NumberOfPages constraintAnnotation) {
        documentCategory = constraintAnnotation.category();
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(DocumentForm documentForm, ConstraintValidatorContext constraintValidatorContext) {
        List<MultipartFile> files = documentForm.getDocuments();

        if (documentForm instanceof DocumentFinancialForm) {
            if (Boolean.TRUE.equals(((DocumentFinancialForm) documentForm).getNoDocument())) {
                return true;
            } else if (files.size() == 0) {
                return true;
            }
        } else if (documentForm instanceof DocumentFinancialGuarantorNaturalPersonForm) {
            if (Boolean.TRUE.equals(((DocumentFinancialGuarantorNaturalPersonForm) documentForm).getNoDocument())) {
                return true;
            } else if (files.size() == 0) {
                return true;
            }
        } else if (documentForm instanceof DocumentTaxForm) {
            if (((DocumentTaxForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.MY_PARENTS
                    || ((DocumentTaxForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.LESS_THAN_YEAR) {
                return true;
            } else if (((DocumentTaxForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX
                    && Boolean.TRUE.equals(((DocumentTaxForm) documentForm).getNoDocument())) {
                return true;
            } else if (files.size() == 0) {
                return true;
            }
        } else if (documentForm instanceof DocumentTaxGuarantorNaturalPersonForm) {
            if (((DocumentTaxGuarantorNaturalPersonForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.MY_PARENTS
                    || ((DocumentTaxGuarantorNaturalPersonForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.LESS_THAN_YEAR) {
                return true;
            } else if (((DocumentTaxGuarantorNaturalPersonForm) documentForm).getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX
                    && Boolean.TRUE.equals(((DocumentTaxGuarantorNaturalPersonForm) documentForm).getNoDocument())) {
                return true;
            } else if (files.size() == 0) {
                return true;
            }
        }
        //region Counting total new pages
        ByteArrayOutputStream byteArrayOutputStream = Utility.mergeMultipartFiles(files.stream().filter(f -> !f.isEmpty()).collect(Collectors.toList()));
        if (byteArrayOutputStream.size() == 0) {
            log.error("Number of new pages [0], max = [" + max + "] for document [" + documentCategory.name() + "]");
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(RESPONSE)
                    .addPropertyNode(PAGES).addConstraintViolation();
            return false;
        }
        int numberOfNewPages = Utility.countNumberOfPagesOfPdfDocument(byteArrayOutputStream.toByteArray());
        if (numberOfNewPages == 0) {
            log.error("Number of new pages [0], max = [" + max + "] for document [" + documentCategory.name() + "]");
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(RESPONSE)
                    .addPropertyNode(PAGES).addConstraintViolation();
            return false;
        }
        //endregion

        //region Counting total old pages
        int numberOfOldPages = 0;
        Tenant tenant = tenantService.findById(documentForm.getTenantId());
        if (documentForm instanceof DocumentGuarantorFormAbstract) {
            Long guarantorId = ((DocumentGuarantorFormAbstract) documentForm).getGuarantorId();
            if (documentForm instanceof DocumentFinancialGuarantorNaturalPersonForm) {
                Long documentId = ((DocumentFinancialGuarantorNaturalPersonForm) documentForm).getDocumentId();
                if (documentId != null) {
                    numberOfOldPages = numberOfPagesByDocumentIdAndGuarantorIdAndTenant(documentId, guarantorId, tenant);
                } else {
                    numberOfOldPages = numberOfPagesByCategoryAndGuarantorIdAndTenant(DocumentCategory.FINANCIAL, guarantorId, tenant);
                }
            } else {
                numberOfOldPages = numberOfPagesByCategoryAndGuarantorIdAndTenant(documentCategory, guarantorId, tenant);
            }
        } else {
            if (documentForm instanceof DocumentFinancialForm) {
                Long documentId = ((DocumentFinancialForm) documentForm).getId();
                if (documentId != null) {
                    numberOfOldPages = numberOfPagesByDocumentIdAndTenant(documentId, tenant);
                } else {
                    numberOfOldPages = numberOfPagesByCategoryAndTenant(DocumentCategory.FINANCIAL, tenant);
                }
            } else {
                numberOfOldPages = numberOfPagesByCategoryAndTenant(documentCategory, tenant);
            }
        }
        //endregion

        int totalPages = numberOfNewPages + numberOfOldPages;
        if (totalPages <= max) {
            log.info("Number of new pages [" + numberOfNewPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
            log.info("Number of old pages [" + numberOfOldPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
            log.info("Validation with result [" + (totalPages <= max) + "], total = [" + totalPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
        } else {
            log.error("Number of new pages [" + numberOfNewPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
            log.error("Number of old pages [" + numberOfOldPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
            log.error("Validation with result [" + (totalPages <= max) + "], total = [" + totalPages + "], max = [" + max + "] for document [" + documentCategory.name() + "]");
        }
        return totalPages <= max;
    }

    private int numberOfPagesByCategoryAndTenant(DocumentCategory documentCategory, Tenant tenant) {
        Integer result = fileRepository.countNumberOfPagesByDocumentCategoryAndTenant(documentCategory, tenant);
        return result == null ? 0 : result;
    }

    private int numberOfPagesByDocumentIdAndTenant(Long documentId, Tenant tenant) {
        Integer result = fileRepository.countNumberOfPagesByDocumentIdAndTenant(documentId, tenant);
        return result == null ? 0 : result;
    }

    private int numberOfPagesByCategoryAndGuarantorIdAndTenant(DocumentCategory documentCategory, Long guarantorId, Tenant tenant) {
        Integer result = fileRepository.countNumberOfPagesByDocumentCategoryAndGuarantorIdAndTenant(documentCategory, guarantorId, tenant);
        return result == null ? 0 : result;
    }

    private int numberOfPagesByDocumentIdAndGuarantorIdAndTenant(Long documentId, Long guarantorId, Tenant tenant) {
        Integer result = fileRepository.countNumberOfPagesByDocumentIdAndGuarantorIdAndTenant(documentId, guarantorId, tenant);
        return result == null ? 0 : result;
    }
}
