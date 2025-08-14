package fr.dossierfacile.process.file.service.documentrules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import static fr.dossierfacile.process.file.service.documentrules.validator.scholarship.ScholarShipHelper.getOptionalValue;

@Slf4j
public class ScholarshipRuleAmountValidity extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_SCHOLARSHIP_AMOUNT;
    }

    @Override
    protected boolean isValid(Document document) {
        var scholarshipFile = getOptionalValue(document);
        if (scholarshipFile.isEmpty()) {
            log.info("Scholarship file not found for document: {}", document.getId());
            return false; // If no scholarship file, we consider the rule invalid
        }
        var safeScholarshipFile = scholarshipFile.get();

        double monthlyAverage = ((double) safeScholarshipFile.getAnnualAmount()) / 10.0;
        // Check amount difference
        double diffAmount = Math.abs(monthlyAverage - document.getMonthlySum());
        return (diffAmount <= 10);

    }
}
