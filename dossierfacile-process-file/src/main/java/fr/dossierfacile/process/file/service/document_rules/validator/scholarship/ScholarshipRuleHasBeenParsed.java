package fr.dossierfacile.process.file.service.document_rules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;

import static fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarShipHelper.getOptionalValue;

public class ScholarshipRuleHasBeenParsed extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_SCHOLARSHIP_PARSED;
    }

    @Override
    protected boolean isValid(Document document) {
        var scholarshipFile = getOptionalValue(document);
        return scholarshipFile.isPresent();
    }
}
