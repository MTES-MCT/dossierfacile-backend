package fr.dossierfacile.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassificationValidatorB extends BaseDocumentIAValidator {

    private final String documentType;

    public ClassificationValidatorB(String documentType) {
        this.documentType = documentType;
    }

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_DOCUMENT_IA_CLASSIFICATION;
    }

    @Override
    protected boolean isValid(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        if (documentIAAnalyses.isEmpty()) {
            return false;
        }
        var allGoodClassification = true;
        for (var analysis : documentIAAnalyses) {
            var classification = analysis.getResult().getClassification();
            if (classification != null && !classification.getDocumentType().equals(documentType)) {
                allGoodClassification = false;
                break;
            }
        }
        return allGoodClassification;
    }
}
