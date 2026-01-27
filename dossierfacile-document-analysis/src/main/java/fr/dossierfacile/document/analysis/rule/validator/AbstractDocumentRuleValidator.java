package fr.dossierfacile.document.analysis.rule.validator;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;

public abstract class AbstractDocumentRuleValidator {

    protected abstract boolean isValid(Document document);

    protected abstract boolean isBlocking();

    protected abstract boolean isInconclusive();

    protected abstract DocumentRule getRule();

    public RuleValidatorOutput validate(Document document) {
        boolean isValid = isValid(document);
        boolean isBlocking = isBlocking();
        boolean isInconclusive = isInconclusive();
        DocumentRule rule = getRule();

        if (!isValid) {
            if (isInconclusive) {
                return new RuleValidatorOutput(false, isBlocking, DocumentAnalysisRule.documentInconclusiveRuleFrom(rule), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
            } else {
                return new RuleValidatorOutput(false, isBlocking, DocumentAnalysisRule.documentFailedRuleFrom(rule), RuleValidatorOutput.RuleLevel.FAILED);
            }
        } else {
            return new RuleValidatorOutput(true, isBlocking, DocumentAnalysisRule.documentPassedRuleFrom(rule), RuleValidatorOutput.RuleLevel.PASSED);
        }
    }
}
