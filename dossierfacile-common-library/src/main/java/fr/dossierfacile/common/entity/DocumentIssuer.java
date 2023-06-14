package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentIssuer {

    MON_FRANCE_CONNECT("Mon FranceConnect"),
    PAYFIT("PayFit"),
    DGFIP("DGFIP"),
    UNKNOWN("Unknown"),
    ;

    private final String label;

}
