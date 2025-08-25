package fr.dossierfacile.process.file.service.document_rules.validator.rental;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RentalRuleNumberOfPage extends AbstractDocumentRuleValidator {

    static final int MIN_NUMBER_OF_RECEIPT = 3;

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS;
    }

    @Override
    protected boolean isValid(Document document) {
        if (document.getDocumentCategoryStep() != DocumentCategoryStep.TENANT_PROOF && !checkNumberOfPage(document)) {
            log.info("Document number of pages mismatches :{}", document.getId());
            return false;
        } else {
            return true;
        }
    }

    private boolean checkNumberOfPage(Document document) {
        return document.getGuarantor() != null || document.getFiles().stream().mapToInt(File::getNumberOfPages).sum() >= MIN_NUMBER_OF_RECEIPT;
    }
}
