package fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HasBeenDocumentIAAnalysedBI extends BaseDocumentIAValidator {

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
        return DocumentRule.R_DOCUMENT_IA_ANALYSED;
    }

    @Override
    protected boolean isValid(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        return !documentIAAnalyses.isEmpty();
    }
}