package fr.dossierfacile.common.enums;

public enum ActionType {
    ACTION1("INCOMPLETE -> TO_PROCESS"),
    ACTION2("TO_PROCESS -> DECLINE"),
    ACTION3("TO_PROCESS -> VALIDATED"),
    ACTION4("DECLINE -> TO_PROCESS");

    String label;

    ActionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
