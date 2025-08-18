package fr.dossierfacile.process.file.service.document_rules.validator.rental;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;

public class RentalHasBeenParsed extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_RENT_RECEIPT_PARSED;
    }

    @Override
    protected boolean isValid(Document document) {
        return document.getFiles().stream().noneMatch(f -> f.getParsedFileAnalysis() == null || f.getParsedFileAnalysis().getParsedFile() == null);
    }
}
