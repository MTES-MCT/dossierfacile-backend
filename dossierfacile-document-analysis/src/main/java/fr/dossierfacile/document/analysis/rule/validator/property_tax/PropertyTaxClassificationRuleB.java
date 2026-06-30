package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.document.analysis.rule.validator.tax.BaseTaxRule;

public class PropertyTaxClassificationRuleB extends BaseTaxRule {

    private static final String DOCUMENT_TYPE = "taxe_fonciere";

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
        var atLeastOneGoodClassification = false;
        for (var analysis : documentIAAnalyses) {
            var classification = analysis.getResult().getClassification();
            if (classification != null && classification.getDocumentType().equals(DOCUMENT_TYPE)) {
                atLeastOneGoodClassification = true;
                break;
            }
        }
        return atLeastOneGoodClassification;
    }
}
