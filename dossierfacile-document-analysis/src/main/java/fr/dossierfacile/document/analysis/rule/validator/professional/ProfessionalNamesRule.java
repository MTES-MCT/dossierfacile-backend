package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.util.IdentityMatchUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*
 * Rule R_PROFESSIONAL_NAME_MATCH:
 *
 * Cette règle vérifie que l'identité présente dans le 2D-Doc d'un document d'activité professionnelle
 * correspond à l'identité du locataire (ou du garant si le document lui appartient).
 *
 * Données examinées:
 * - Les propriétés `liste_prenoms` et `nom_patronymique` contenues dans les barcodes 2D-Doc du document.
 * - L'identité attendue du locataire ou du garant (`firstName`, `lastName`, `preferredName`).
 *
 * Fonctionnement:
 * 1) Récupération de l'identité du locataire ou garant rattaché au document (`getNamesFromDocument`).
 * 2) Extraction des identités des barcodes 2D-Doc à partir de `liste_prenoms` et `nom_patronymique`.
 * 3) Si aucune analyse réussie, aucun barcode exploitable ou identité manquante -> retourne INCONCLUSIVE.
 * 4) Comparaison du nom de famille (nom de naissance + nom d'usage) via `IdentityMatchUtil.hasLastNameMatch`.
 * 5) Comparaison des prénoms via `IdentityMatchUtil.hasFirstNameMatch`.
 * 6) Si nom ET prénom correspondent -> retourne PASSED.
 * 7) Sinon -> retourne FAILED.
 */
public class ProfessionalNamesRule extends BaseDocumentIAValidator {

    private static final String EXPECTED_DOC_TYPE = "29";

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PROFESSIONAL_NAME_MATCH;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        var nameToMatch = getNamesFromDocument(document);

        NamesRuleData namesRuleData = null;
        if (nameToMatch != null) {
            var expectedName = new NamesRuleData.Name(
                    nameToMatch.getFirstNamesAsString(),
                    nameToMatch.getLastName(),
                    nameToMatch.getPreferredName()
            );

            namesRuleData = new NamesRuleData(expectedName, List.of());
        }

        var barcodes = documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .filter(Objects::nonNull)
                .filter(result -> result.getBarcodes() != null)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(Objects::nonNull)
                .filter(barcode -> EXPECTED_DOC_TYPE.equals(barcode.getDocType()))
                .toList();

        if (barcodes.isEmpty() || nameToMatch == null) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        var extractedNames = barcodes.stream()
                .map(this::convertBarcodeModelToExtractedName)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        var listOfBarcodeIdentities = extractedNames.stream()
                .map(name -> (name.firstNames() + " " + name.lastName()).trim())
                .filter(s -> !s.isBlank())
                .toList();

        namesRuleData = new NamesRuleData(namesRuleData != null ? namesRuleData.expectedName() : null, extractedNames);

        if (listOfBarcodeIdentities.isEmpty()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        var hasLastNameMatch = IdentityMatchUtil.hasLastNameMatch(listOfBarcodeIdentities, nameToMatch);
        if (!hasLastNameMatch) {
            return reject(namesRuleData);
        }

        var hasFirstNameMatch = IdentityMatchUtil.hasFirstNameMatch(listOfBarcodeIdentities, nameToMatch);
        if (!hasFirstNameMatch) {
            return reject(namesRuleData);
        }

        return new RuleValidatorOutput(
                true,
                isBlocking(),
                DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData),
                RuleValidatorOutput.RuleLevel.PASSED
        );
    }

    private RuleValidatorOutput reject(NamesRuleData ruleData) {
        return new RuleValidatorOutput(
                false,
                isBlocking(),
                DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData),
                RuleValidatorOutput.RuleLevel.FAILED
        );
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private Optional<NamesRuleData.Name> convertBarcodeModelToExtractedName(BarcodeModel barcodeModel) {
        if (barcodeModel == null || barcodeModel.getTypedData() == null) {
            return Optional.empty();
        }

        var listePrenoms = barcodeModel.getTypedData().stream()
                .filter(data -> data != null && "liste_prenoms".equals(data.getName()))
                .map(GenericProperty::getStringValue)
                .filter(Objects::nonNull)
                .findFirst();

        var nomPatronymique = barcodeModel.getTypedData().stream()
                .filter(data -> data != null && "nom_patronymique".equals(data.getName()))
                .map(GenericProperty::getStringValue)
                .filter(Objects::nonNull)
                .findFirst();

        if (listePrenoms.isEmpty() && nomPatronymique.isEmpty()) {
            return Optional.empty();
        }

        String prenom = listePrenoms.orElse("");
        String nom = nomPatronymique.orElse("");

        return Optional.of(new NamesRuleData.Name(prenom, nom, null));
    }
}
