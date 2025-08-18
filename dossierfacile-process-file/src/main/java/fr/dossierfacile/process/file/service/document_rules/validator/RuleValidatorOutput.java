package fr.dossierfacile.process.file.service.document_rules.validator;

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
