package fr.dossierfacile.common.enums;

public enum DocumentAutoValidationReason {
    VALIDATED,
    DOCUMENT_NOT_ELIGIBLE,
    REPORT_MISSING,
    REPORT_NOT_CHECKED,
    FAILED_RULES_PRESENT,
    INCONCLUSIVE_RULES_PRESENT,
    NO_PASSED_RULES
}
