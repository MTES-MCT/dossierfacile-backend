package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.TaxNamesRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.document_ia_model.PropertyTaxModel;
import fr.dossierfacile.document.analysis.rule.validator.util.IdentityMatchUtil;

import java.util.List;

/*
 * Rule R_PROPERTY_TAX_NAMES:
 *
 * Checks that the owner identity extracted from the property tax notice (taxe foncière) matches the
 * candidate identity (tenant or guarantor). The owner identity comes from the "proprietaire_identite"
 * field, a single free-form string (e.g. "DUPONT Camille"), so we reuse IdentityMatchUtil free-form
 * matching, exactly like TaxNamesRule does for declarant_1 / declarant_2.
 *
 * Decisions:
 * - The rule is blocking; the owner identity is mandatory. If it is not extracted, the document is
 *   refused (FAILED) — see the product decision "missing data -> refusal".
 * - Only a dossier without identity yields INCONCLUSIVE (data integrity, not the document's fault).
 *
 * Known limitation: only the recipient identity (destinataire) is extracted; co-owners possibly
 * listed on page 2 are ignored for now.
 */
public class PropertyTaxNamesRule extends BaseDocumentIAValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PROPERTY_TAX_NAMES;
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

        // No candidate identity in the dossier: cannot conclude.
        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var extractedOwner = new DocumentIAMergerMapper()
                .map(documentIAAnalyses, PropertyTaxModel.class)
                .map(model -> model.proprietaireIdentite)
                .filter(identity -> !identity.isBlank())
                .orElse(null);

        // The owner identity was not extracted: refused (decision "missing data -> refusal").
        if (extractedOwner == null) {
            return reject(namesRuleData);
        }

        var extractedIdentities = List.of(extractedOwner);
        namesRuleData = new TaxNamesRuleData(namesRuleData, extractedIdentities);

        var hasLastNameMatch = IdentityMatchUtil.hasLastNameMatch(extractedIdentities, nameToMatch);
        if (!hasLastNameMatch) {
            return reject(namesRuleData);
        }

        var hasFirstNameMatch = IdentityMatchUtil.hasFirstNameMatch(extractedIdentities, nameToMatch);
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
}
