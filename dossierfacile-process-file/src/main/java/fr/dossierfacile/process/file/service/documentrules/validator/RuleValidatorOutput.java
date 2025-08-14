package fr.dossierfacile.process.file.service.documentrules.validator;

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
