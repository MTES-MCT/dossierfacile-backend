package fr.dossierfacile.document.analysis.rule.validator.document_ia;

public enum DocumentIAPropertyType {
    STRING("string"),
    DATE("date");

    private final String label;

    DocumentIAPropertyType(String label) {
        this.label = label;
    }


}
