package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.TaxClassificationRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;

import java.util.List;

public class TaxClassificationRuleB extends BaseTaxRule {

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
        return DocumentRule.R_TAX_BAD_CLASSIFICATION;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        List<BarcodeModel> taxOrDeclarativeSituations = documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(this::isTaxOrDeclarativeSituation)
                .toList();

        // If there is no tax or declarative situation found
        // We return a failed validation because it's a complete misclassification
        if (taxOrDeclarativeSituations.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), new TaxClassificationRuleData(false)), RuleValidatorOutput.RuleLevel.FAILED);
        }

        var containsTax = taxOrDeclarativeSituations.stream().anyMatch(this::isTax);
        var containsDeclarativeSituation = taxOrDeclarativeSituations.stream().anyMatch(this::isDeclarativeSituation);

        // If it contains at least one tax assessment, we consider the document correctly classified
        if (containsTax) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.PASSED);
        }

        // If it contains only declarative situations, we consider it misclassified with a custom rule
        if (containsDeclarativeSituation) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), new TaxClassificationRuleData(true)), RuleValidatorOutput.RuleLevel.FAILED);
        }

        return super.validate(document);

    }


    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
