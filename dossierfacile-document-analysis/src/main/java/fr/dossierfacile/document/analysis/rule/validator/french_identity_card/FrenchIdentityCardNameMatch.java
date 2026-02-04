package fr.dossierfacile.document.analysis.rule.validator.french_identity_card;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;
import fr.dossierfacile.document.analysis.util.NameUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FrenchIdentityCardNameMatch extends BaseDocumentIAValidator {

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
        return DocumentRule.R_FRENCH_IDENTITY_CARD_NAME_MATCH;
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

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isNameMatch = false;

        var extractedIdentity = new DocumentIAMergerMapper().map(documentIAAnalyses, DocumentIdentity.class);

        if (extractedIdentity.isPresent() && extractedIdentity.get().isValid()) {
            isNameMatch = NameUtil.isNameMatching(nameToMatch, extractedIdentity.get());
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var listOfExtractedNames = List.of(
                new NamesRuleData.Name(
                        extractedIdentity.get().getFirstNamesAsString(),
                        extractedIdentity.get().getLastName(),
                        extractedIdentity.get().getPreferredName()
                )
        );

        namesRuleData = new NamesRuleData(
                namesRuleData,
                listOfExtractedNames
        );

        if (isNameMatch) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
