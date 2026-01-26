package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentRule {

    R_TAX_2D_DOC(
            DocumentRuleLevel.INFO,
            "",
            "Le 2D Doc de l'avis d'imposition est lisible",
            "Le document n'a pas de 2D DOC ou il n'est pas lisible"
    ),
    R_TAX_PARSE(
            DocumentRuleLevel.INFO,
            "",
            "La lecture des informations de l'avis a réussi",
            "La lecture des informations de l'avis a échoué"
    ),
    R_TAX_BAD_CLASSIFICATION(
            DocumentRuleLevel.CRITICAL,
            "Le document n'est pas un avis d'imposition",
            "Le document est bien un avis d'imposition",
            ""
    ),
    R_TAX_FAKE(
            DocumentRuleLevel.CRITICAL,
            "Les informations sont floues ou corrompues",
            "Les informations sont claires et non corrompues",
            ""
    ),
    R_TAX_N1(
            DocumentRuleLevel.CRITICAL,
            "L'avis d'imposition sur les revenus N-1 doit etre fournis",
            "L'avis d'imposition sur les revenus N-1 a été fourni",
            ""
    ),
    R_TAX_LEAF(
            DocumentRuleLevel.CRITICAL,
            "Veuillez fournir les feuillets des avis",
            "Tous les feuillets des avis ont été fournis",
            ""
    ),
    R_TAX_N3(
            DocumentRuleLevel.CRITICAL,
            "Les avis d'imposition antérieur à N-3 ne sont pas autorisé",
            "Les avis d'imposition sont dans la période autorisée",
            ""
    ),
    R_TAX_NAMES(
            DocumentRuleLevel.CRITICAL,
            "Les noms et prénoms ne correspondent pas",
            "Les noms et prénoms correspondent",
            ""
    ),

    R_GUARANTEE_NAMES(
            DocumentRuleLevel.CRITICAL,
            "Les noms et prénoms ne correspondent pas",
            "Les noms et prénoms correspondent",
            ""
    ),
    R_GUARANTEE_EXPIRED(
            DocumentRuleLevel.CRITICAL,
            "La garantie a expiré",
            "La garantie est valide",
            ""
    ),
    R_GUARANTEE_PARSING(
            DocumentRuleLevel.INFO,
            "",
            "La lecture des informations de la garantie a réussi",
            "La lecture des informations de la garantie a échoué"
    ),
    R_PAYSLIP_PARSING(
            DocumentRuleLevel.INFO,
            "",
            "La lecture des informations du bulletin de salaire a réussi",
            "La lecture des informations du bulletin de salaire a échoué"
    ),
    R_PAYSLIP_QRCHECK(
            DocumentRuleLevel.CRITICAL,
            "La lecture des informations et du QR Code ne correspondent pas",
            "Les informations et le QR Code correspondent",
            ""
    ),
    R_PAYSLIP_NAME(
            DocumentRuleLevel.CRITICAL,
            "Nom/prénoms ne correspondent pas",
            "Nom/prénoms correspondent",
            ""
    ),
    R_PAYSLIP_MONTHS(
            DocumentRuleLevel.CRITICAL,
            "Les 3 derniers bulletins de salaire doivent être transmis",
            "Les 3 derniers bulletins de salaire ont été transmis",
            ""
    ),
    R_PAYSLIP_AMOUNT_MISMATCHES(
            DocumentRuleLevel.CRITICAL,
            "Le montant specifié ne correspond pas au montant des bulletins",
            "Le montant spécifié correspond au montant des bulletins",
            ""
    ),
    R_SCHOLARSHIP_PARSED(
            DocumentRuleLevel.INFO,
            "",
            "Le document a été analysé",
            "Le document n'a pas pu être analysé"
    ),
    R_SCHOLARSHIP_NAME(
            DocumentRuleLevel.CRITICAL,
            "Le nom/prénom sur la notification de bourse ne correspond pas",
            "Le nom/prénom sur la notification de bourse correspond",
            ""
    ),
    R_SCHOLARSHIP_EXPIRED(
            DocumentRuleLevel.CRITICAL,
            "La date de la bourse ne correspond plus",
            "La date de la bourse est valide",
            ""
    ),
    R_SCHOLARSHIP_AMOUNT(
            DocumentRuleLevel.CRITICAL,
            "Le montant de la bourse ne correspond pas au montant déclaré",
            "Le montant de la bourse correspond au montant déclaré",
            ""
    ),

    R_RENT_RECEIPT_NAME(
            DocumentRuleLevel.CRITICAL,
            "Le nom et le prénom ne correspondent pas",
            "Le nom et le prénom correspondent",
            ""
    ),
    R_RENT_RECEIPT_PARSED(
            DocumentRuleLevel.INFO,
            "",
            "Le document a été analysé",
            "Le document n'a pas pu être analysé"
    ),
    R_RENT_RECEIPT_MONTHS(
            DocumentRuleLevel.CRITICAL,
            "Les quittances sont trop anciennes",
            "Les quittances sont récentes",
            ""
    ),
    R_RENT_RECEIPT_NB_DOCUMENTS(
            DocumentRuleLevel.CRITICAL,
            "Un seul document a été détecté",
            "Le nombre de documents requis a été détecté",
            ""
    ),

    R_FRANCE_IDENTITE_NAMES(
            DocumentRuleLevel.CRITICAL,
            "Les noms et prénoms ne correspondent pas",
            "Les noms et prénoms correspondent",
            ""
    ),
    R_FRANCE_IDENTITE_STATUS(
            DocumentRuleLevel.INFO,
            "",
            "Ce document a été validé par France Identité",
            "Ce document n'a pas pu être validé par France Identité"
            ),
    R_BLURRY_FILE_ANALYSED(
            DocumentRuleLevel.INFO,
            "",
            "Le document a été analysé pour la détection de flou",
            "Impossible de déterminer si le document est flou ou non"
    ),
    R_BLURRY_FILE_BLANK(
            DocumentRuleLevel.INFO,
            "",
            "Le document n'est pas vide",
            "Le document semble vide"
    ),
    R_BLURRY_FILE(
            DocumentRuleLevel.WARN,
            "Votre document semble flou",
            "Votre document semble net et lisible",
            ""
    ),
    R_DOCUMENT_IA_ANALYSED(
            DocumentRuleLevel.INFO,
            "",
            "Le document a été analysé par Document IA",
            "Le document n'a pas pu être analysé par Document IA"
    ),
    R_DOCUMENT_IA_CLASSIFICATION(
            DocumentRuleLevel.CRITICAL,
            "Le document n'a pas été correctement classifié par Document IA",
            "Le document a été correctement classifié par Document IA",
            ""
    ),
    R_DOCUMENT_IA_OTHER_DOCUMENTS(
            DocumentRuleLevel.WARN,
            "Le document comprends des éléments inattendus",
            "Tous les fichiers sont de la bonne catégorie",
            ""
    ),
    R_FRENCH_IDENTITY_CARD_NAME_MATCH(
            DocumentRuleLevel.CRITICAL,
            "Le nom et le prénom sur la carte d'identité ne correspondent pas",
            "Le nom et le prénom sur la carte d'identité correspondent",
            "Impossible de vérifier le nom et le prénom sur la carte d'identité"
    ),
    R_FRENCH_IDENTITY_CARD_EXPIRATION(
            DocumentRuleLevel.CRITICAL,
            "La carte d'identité est expirée",
            "La carte d'identité est valide",
            "Impossible de vérifier la date de validité de la carte d'identité"
    ),
    R_PAYSLIP_CONTINUITY(
            DocumentRuleLevel.CRITICAL,
            "Les bulletins de salaire ne sont pas continus",
            "Les bulletins de salaire sont continus",
            "Impossible de vérifier la continuité des bulletins de salaire"
    ),
    R_PAYSLIP_NAME_MATCH(
            DocumentRuleLevel.CRITICAL,
            "Le nom et le prénom sur les bulletins de salaire ne correspondent pas",
            "Le nom et le prénom sur les bulletins de salaire correspondent",
            "Impossible de vérifier le nom et le prénom sur les bulletins de salaire"
    );

    private final DocumentRuleLevel level;
    private final String failedMessage;
    private final String passedMessage;
    private final String inconclusiveMessage;

}

