package fr.dossierfacile.common.enums;

import lombok.Getter;

@Getter
public enum DocumentCategoryStep {

    // Residency
    TENANT_RECEIPT("document_category_step.TENANT_RECEIPT"),
    TENANT_PROOF("document_category_step.TENANT_PROOF"),
    GUEST_PROOF("document_category_step.GUEST_PROOF"),
    GUEST_NO_PROOF("document_category_step.GUEST_NO_PROOF");

    final String label;

    DocumentCategoryStep(String label) {
        this.label = label;
    }

}
