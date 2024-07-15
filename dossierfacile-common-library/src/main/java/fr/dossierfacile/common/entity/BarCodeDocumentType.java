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
    THALES_PAYSLIP("Fiche de paie Thalès"),
    UNKNOWN_PAYSLIP("Fiche de paie non-identifiée"),

    FREE_INVOICE("Facture Free"),

    CVEC("Contribution vie étudiante"),

    UNKNOWN("Unknown");

    private final String label;

}
