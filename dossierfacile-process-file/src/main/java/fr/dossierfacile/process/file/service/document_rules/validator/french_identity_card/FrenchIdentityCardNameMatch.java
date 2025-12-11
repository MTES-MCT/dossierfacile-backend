package fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card.document_ia_model.DocumentIdentity;
import fr.dossierfacile.process.file.util.NameUtil;
import lombok.extern.slf4j.Slf4j;

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

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var nameToMatch = getNamesFromDocument(document);

        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isNameMatch = false;

        var extractedIdentity = DocumentIAMergerMapper.map(documentIAAnalyses, DocumentIdentity.class);

        if (extractedIdentity.isPresent() && extractedIdentity.get().isValid()) {
            isNameMatch = NameUtil.isNameMatching(nameToMatch, extractedIdentity.get());
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (isNameMatch) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.FAILED);
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
