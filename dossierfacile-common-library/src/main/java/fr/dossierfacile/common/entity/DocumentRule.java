package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentRule {

    R_TAX_PARSE(Level.WARN, "La lecture des informations de l'avis a échoué"),
    R_TAX_BAD_CLASSIFICATION(Level.CRITICAL, "Le document n'est pas un avis d'imposition"),
    R_TAX_FAKE(Level.CRITICAL, "Les informations sont floues ou corrompues"),
    R_TAX_N1(Level.CRITICAL, "L'avis d'imposition sur les revenus N-1 doit etre fournis"),
    R_TAX_LEAF(Level.CRITICAL, "Veuillez fournir les feuillets des avis"),
    R_TAX_ALL_LEAF(Level.WARN, "Veuillez fournir tous les feuillets des avis"),// feuillet 1 founi
    R_TAX_N3(Level.CRITICAL, "Les avis d'imposition antérieur à N-3 ne sont pas autorisé"),
    R_TAX_NAMES(Level.CRITICAL, "Les noms et prénoms ne correspondent pas"),

    R_GUARANTEE_NAMES(Level.CRITICAL, "Les noms et prénoms ne correspondent pas"),
    R_GUARANTEE_EXPIRED(Level.CRITICAL, "La garantie a expiré"),

    R_PAYSLIP_QRCHECK(Level.CRITICAL, "La lecture des informations et du QR Code ne correspondent pas"),
    R_PAYSLIP_NAME(Level.CRITICAL, "Nom/prénoms ne correspondent pas"),
    R_PAYSLIP_MONTHS(Level.CRITICAL, "Les 3 derniers bulletins de salaire doivent être transmis"),
    R_PAYSLIP_AMOUNT_MISMATCHES(Level.CRITICAL, "Le montant specifié ne correspond pas au montant des bulletins"),

    R_SCHOLARSHIP_NAME(Level.CRITICAL, "Le nom/prénom sur la notification de bourse ne correspond pas"),
    R_SCHOLARSHIP_EXPIRED(Level.CRITICAL, "La date de la bourse ne correspond plus"),
    R_SCHOLARSHIP_AMOUNT(Level.CRITICAL, "Le montant de la bourse ne correspond pas au montant déclaré"),

    R_RENT_RECEIPT_NAME(Level.CRITICAL, "Le nom et le prénom ne correspondent pas"),
    R_RENT_RECEIPT_MONTHS(Level.CRITICAL, "Les quittances sont trop anciennes"),
    R_RENT_RECEIPT_ADDRESS_SALARY(Level.WARN, "TODO. L'adresse de la location semble ne pas correspondre à l'adresse des bulletins de payes"),
    R_RENT_RECEIPT_NB_DOCUMENTS(Level.WARN, "Un seul document a été détecté"),

    R_FRANCE_IDENTITE_NAMES(Level.CRITICAL, "Les noms et prénoms ne correspondent pas"),
    R_FRANCE_IDENTITE_STATUS(Level.CRITICAL, "Ce document n'a pas pu être validé par France Identité"),

    R_BLURRY_FILE(Level.WARN, "Le document est flou");

    public enum Level {
        CRITICAL, WARN
    }

    private final Level level;
    private final String defaultMessage;

}

