package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.util.FilePageCounter;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidatorContext;
import java.io.IOException;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.LESS_THAN_YEAR;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_PARENTS;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_RESIDENCY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_TAX;
import static java.lang.Boolean.TRUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class NumberOfPagesValidator extends TenantConstraintValidator<NumberOfPages, DocumentForm> {

    private static final String PAGES = "documents";
    private static final String RESPONSE = "The number of new pages must be greater than 0";

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

        if (documentForm instanceof DocumentFinancialForm form) {
            if (TRUE.equals(form.getNoDocument()) || files.isEmpty()) {
                return true;
            }
        } else if (documentForm instanceof DocumentResidencyForm form) {
            if (form.getTypeDocumentResidency() == OTHER_RESIDENCY) {
                return true;
            }
        } else if (documentForm instanceof DocumentFinancialGuarantorNaturalPersonForm form) {
            if (TRUE.equals(form.getNoDocument()) || files.isEmpty()) {
                return true;
            }
        } else if (documentForm instanceof DocumentResidencyGuarantorNaturalPersonForm form) {
            if (form.getTypeDocumentResidency() == OTHER_RESIDENCY) {
                return true;
            }
        } else if (documentForm instanceof DocumentTaxForm form) {
            DocumentSubCategory subCategory = form.getTypeDocumentTax();
            if (subCategory == MY_PARENTS || subCategory == LESS_THAN_YEAR) {
                return true;
            } else if (subCategory == OTHER_TAX && TRUE.equals(form.getNoDocument())) {
                return true;
            } else if (files.isEmpty()) {
                return true;
            }
        } else if (documentForm instanceof DocumentTaxGuarantorNaturalPersonForm form) {
            DocumentSubCategory subCategory = form.getTypeDocumentTax();
            if (subCategory == MY_PARENTS || subCategory == LESS_THAN_YEAR) {
                return true;
            } else if (subCategory == OTHER_TAX && TRUE.equals(form.getNoDocument())) {
                return true;
            } else if (files.isEmpty()) {
                return true;
            }
        }

        //region Counting total new pages
        int numberOfNewPages = 0;
        try {
            numberOfNewPages = new FilePageCounter(files).getTotalNumberOfPages();
        } catch (IOException e) {
            log.error("Can't count files total number of pages", e);
        }
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
        Tenant tenant = getTenant(documentForm);
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
