package fr.dossierfacile.document.analysis.rule.validator;

import fr.dossierfacile.common.entity.DocumentAnalysisRule;

public record RuleValidatorOutput(
        boolean isValid,
        boolean isBlocking,
        DocumentAnalysisRule rule,
        RuleLevel ruleLevel
) {
    public enum RuleLevel {
        FAILED,
        PASSED,
        INCONCLUSIVE
    }
}
