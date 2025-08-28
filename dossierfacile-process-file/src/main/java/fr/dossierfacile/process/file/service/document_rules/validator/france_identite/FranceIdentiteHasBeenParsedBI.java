package fr.dossierfacile.process.file.service.document_rules.validator.france_identite;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FranceIdentiteHasBeenParsedBI extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_FRANCE_IDENTITE_STATUS;
    }

    @Override
    protected boolean isValid(Document document) {
        var parsedFile = FranceIdentiteHelper.getOptionalValue(document);
        return parsedFile.isPresent();
    }
}