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
 * Checks that the candidate identity (tenant or guarantor) matches one of the owners listed on the
 * property tax notice (taxe foncière). The owners come from the "identites_proprietaires" field, a
 * list of free-form strings (e.g. "DUPONT ANGELIQUE"), so we reuse IdentityMatchUtil free-form
 * matching, exactly like TaxNamesRule does for declarant_1 / declarant_2.
 *
 * The candidate must match at least one owner, which handles joint ownership / couples.
 *
 * Decisions:
 * - The rule is blocking; the owner identities are mandatory. If they are not extracted, the
 *   document is refused (FAILED) — see the product decision "missing data -> refusal".
 * - Only a dossier without identity yields INCONCLUSIVE (data integrity, not the document's fault).
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

        var extractedOwners = new DocumentIAMergerMapper()
                .map(documentIAAnalyses, PropertyTaxModel.class)
                .map(model -> model.identitesProprietaires)
                .map(owners -> owners.stream().filter(owner -> owner != null && !owner.isBlank()).toList())
                .filter(owners -> !owners.isEmpty())
                .orElse(null);

        // The owner identities were not extracted: refused (decision "missing data -> refusal").
        if (extractedOwners == null) {
            return reject(namesRuleData);
        }

        namesRuleData = new TaxNamesRuleData(namesRuleData, extractedOwners);

        // The candidate must match at least one owner of the list.
        var hasLastNameMatch = IdentityMatchUtil.hasLastNameMatch(extractedOwners, nameToMatch);
        if (!hasLastNameMatch) {
            return reject(namesRuleData);
        }

        var hasFirstNameMatch = IdentityMatchUtil.hasFirstNameMatch(extractedOwners, nameToMatch);
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
