package fr.dossierfacile.process.file.service.document_rules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import static fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarShipHelper.getOptionalValue;

@Slf4j
public class ScholarshipRuleYearValidity extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_SCHOLARSHIP_EXPIRED;
    }

    @Override
    protected boolean isValid(Document document) {
        var scholarshipFile = getOptionalValue(document);
        if (scholarshipFile.isEmpty()) {
            log.info("Scholarship file not found for document: {}", document.getId());
            return false; // If no scholarship file, we consider the rule invalid
        }
        var safeScholarshipFile = scholarshipFile.get();

        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 9, 15))) {
            return safeScholarshipFile.getEndYear() >= now.getYear();
        }
        return safeScholarshipFile.getEndYear() > now.getYear();

    }
}
