package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BarCodeDocumentType {

    MON_FRANCE_CONNECT("Mon FranceConnect"),
    PAYFIT_PAYSLIP("Fiche de paie PayFit"),
    TAX_ASSESSMENT("Avis d'imposition"),
    SNCF_PAYSLIP("Fiche de paie SNCF"),
    UNKNOWN("Unknown"),
    ;

    private final String label;

}
