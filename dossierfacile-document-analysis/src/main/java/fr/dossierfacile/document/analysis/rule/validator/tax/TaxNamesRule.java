package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.TaxNamesRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.util.IdentityMatchUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/*
 * Rule R_TAX_NAMES:
 *
 * Cette règle vérifie que l'identité visible dans le 2D-DOC d'un avis d'imposition
 * correspond à l'identité du locataire (ou garant, selon le document porteur).
 *
 * Pourquoi cette logique est spécifique:
 * - Les champs déclarants du 2D-DOC (declarant_1 / declarant_2) sont des chaînes libres,
 *   parfois incomplètes, parfois inversées, et parfois avec plusieurs identités sur un même document.
 * - Le format n'est pas assez fiable pour déduire systématiquement "nom" vs "prénom" via
 *   une découpe naïve du type "premier token = nom".
 * - On doit aussi accepter des variantes usuelles: accents, espaces multiples, nom d'usage
 *   (preferredName).
 *
 * Stratégie de matching appliquée :
 * 1) Extraction des identités brutes
 *    - On récupère les valeurs de declarant_1 et declarant_2 pour chaque barcode fiscal.
 *    - On ignore les valeurs nulles.
 * 2) Construction des données d'audit
 *    - On transforme les identités extraites en objets NamesRuleData.Name pour tracer
 *      ce qui a été comparé (visible dans le report de règle).
 * 3) Vérification du nom (lastName puis preferredName)
 *    - Les candidats côté dossier sont : lastName + preferredName (si présent).
 *    - On normalise les chaînes (trim + uppercase).
 *    - Matching strict : présence d'un candidat dans l'identité extraite complète.
 *    - Sinon fallback fuzzy : distance de Levenshtein token par token.
 * 4) Vérification des prénoms
 *    - Même moteur de comparaison (normalisation + strict + Levenshtein),
 *      mais en comparant les tokens des prénoms du dossier aux tokens extraits.
 * 5) Décision finale
 *    - PASSED uniquement si nom ET prénom matchent.
 *    - FAILED sinon.
 *    - INCONCLUSIVE si aucun barcode fiscal exploitable ou si l'identité dossier est absente.
 *
 * Intention métier :
 * - Rester suffisamment tolérant pour limiter les faux négatifs,
 *   sans perdre la sécurité apportée par une vérification explicite nom + prénom.
 */
public class TaxNamesRule extends BaseTaxRule {

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
        return DocumentRule.R_TAX_NAMES;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        var nameToMatch = getNamesFromDocument(document);

        TaxNamesRuleData namesRuleData = null;
        if (nameToMatch != null) {
            var expectedName = new TaxNamesRuleData.Name(
                    nameToMatch.getFirstNamesAsString(),
                    nameToMatch.getLastName(),
                    nameToMatch.getPreferredName()
            );

            namesRuleData = new TaxNamesRuleData(expectedName, List.of());
        }

        var tax = documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(this::isTax)
                .toList();

        if (tax.isEmpty() || nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var listOfBarcodeIdentities = tax.stream().flatMap(it -> convertBarcodeModelToTaxIdentity(it).stream()).toList();

        namesRuleData = new TaxNamesRuleData(namesRuleData, listOfBarcodeIdentities);

        var hasLastNameMatch = IdentityMatchUtil.hasLastNameMatch(listOfBarcodeIdentities, nameToMatch);

        if (!hasLastNameMatch) {
            return reject(namesRuleData);
        }

        var hasFirstNameMatch = IdentityMatchUtil.hasFirstNameMatch(listOfBarcodeIdentities, nameToMatch);

        if (!hasFirstNameMatch) {
            return reject(namesRuleData);
        }

        return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.PASSED);
    }

    private RuleValidatorOutput reject(TaxNamesRuleData ruleData) {
        return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData), RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private List<String> convertBarcodeModelToTaxIdentity(BarcodeModel barcodeModel) {
        var declarant1Name = barcodeModel.getTypedData().stream().filter(data -> data.getName().equals("declarant_1")).findFirst();
        var declarant2Name = barcodeModel.getTypedData().stream().filter(data -> data.getName().equals("declarant_2")).findFirst();

        if (declarant2Name.isEmpty() && declarant1Name.isEmpty()) {
            return List.of();
        }

        return Stream.of(declarant1Name, declarant2Name)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(GenericProperty::getStringValue)
                .filter(Objects::nonNull)
                .toList();
    }
}
