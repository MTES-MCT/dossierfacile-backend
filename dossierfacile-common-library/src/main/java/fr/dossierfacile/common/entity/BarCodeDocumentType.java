package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BarCodeDocumentType {

    TAX_ASSESSMENT("Avis d'imposition"),
    TAX_DECLARATION("Avis de situation déclarative"),

    PAYFIT_PAYSLIP("Fiche de paie PayFit"),
    SNCF_PAYSLIP("Fiche de paie SNCF"),
    PUBLIC_PAYSLIP("Fiche de paie fonctionnaire"),

    FREE_INVOICE("Facture Free"),

    UNKNOWN("Unknown"),
    ;

    private final String label;

}
