package fr.dossierfacile.api.front.service.document.analysis.rule.validator.french_identity_card;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.utils.NameUtil;
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
        var expectedDatas = new ArrayList<GenericProperty>();
        if (nameToMatch != null) {
            expectedDatas.add(new GenericProperty("firstNames", nameToMatch.getFirstNamesAsString(), "String"));
            expectedDatas.add(new GenericProperty("lastName", nameToMatch.getLastName(), "String"));
            expectedDatas.add(new GenericProperty("preferredName", nameToMatch.getPreferredName() != null ? nameToMatch.getPreferredName() : "N/A", "String"));
        }

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isNameMatch = false;

        var extractedIdentity = new DocumentIAMergerMapper().map(documentIAAnalyses, DocumentIdentity.class);

        if (extractedIdentity.isPresent() && extractedIdentity.get().isValid()) {
            isNameMatch = NameUtil.isNameMatching(nameToMatch, extractedIdentity.get());
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), expectedDatas), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var extractedDatas = new ArrayList<GenericProperty>();
        extractedDatas.add(new GenericProperty("firstNames", extractedIdentity.get().getFirstNamesAsString(), "String"));
        extractedDatas.add(new GenericProperty("lastName", extractedIdentity.get().getLastName(), "String"));
        extractedDatas.add(new GenericProperty("preferredName", extractedIdentity.get().getPreferredName() != null ? extractedIdentity.get().getPreferredName() : "N/A", "String"));

        if (isNameMatch) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), expectedDatas, extractedDatas), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), expectedDatas, extractedDatas), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    private DocumentIdentity getNamesFromDocument(Document document) {
        if (document.getGuarantor() != null) {
            return new DocumentIdentity(List.of(document.getGuarantor().getFirstName()), document.getGuarantor().getLastName());
        }

        if (document.getTenant() != null) {
            return new DocumentIdentity(List.of(document.getTenant().getFirstName()), document.getTenant().getLastName(), document.getTenant().getPreferredName());
        }
        return null;
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
