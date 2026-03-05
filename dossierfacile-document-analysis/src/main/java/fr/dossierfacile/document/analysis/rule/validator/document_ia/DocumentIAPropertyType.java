package fr.dossierfacile.document.analysis.rule.validator.document_ia;

public enum DocumentIAPropertyType {
    STRING("string"),
    LIST_STRING("list_string"),
    LIST_OBJECT("list_object"),
    OBJECT("object"),
    DATE("date");

    private final String label;

    DocumentIAPropertyType(String label) {
        this.label = label;
    }


}
