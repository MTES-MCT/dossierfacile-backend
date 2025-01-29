package fr.dossierfacile.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    public static List<DocumentCategoryStep> alphabeticallySortedValues() {
        List<DocumentCategoryStep> values = Arrays.asList(values());
        values.sort(Comparator.comparing(DocumentCategoryStep::name));
        return values;
    }

}
