package fr.dossierfacile.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;

import java.util.List;

public class OneOffClassificationValidatorB extends BaseDocumentIAValidator {

    private final List<String> documentTypes;

    public OneOffClassificationValidatorB(List<String> documentTypes) {
        this.documentTypes = documentTypes;
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
        var oneGoodClassification = false;
        for (var analysis : documentIAAnalyses) {
            var classification = analysis.getResult().getClassification();
            if (classification != null && documentTypes.contains(classification.getDocumentType())) {
                oneGoodClassification = true;
                break;
            }
        }
        return oneGoodClassification;
    }
}