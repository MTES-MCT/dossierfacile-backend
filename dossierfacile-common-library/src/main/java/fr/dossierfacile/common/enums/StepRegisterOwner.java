package fr.dossierfacile.common.enums;

import lombok.Getter;

@Getter
public enum StepRegisterOwner {
    STEP1("step1"),
    STEP2("step2"),
    STEP3("step3"),
    ;
    private final String label;

    StepRegisterOwner(String label) {
        this.label = label;
    }
}
