package fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentExpiration;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;

import java.util.Optional;

public class PayslipContinuityRule extends BaseDocumentIAValidator {

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
        return DocumentRule.R_FRENCH_IDENTITY_CARD_EXPIRATION;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isCardValid = Optional.of(false);

        var extractedDates = DocumentIAMergerMapper.map(documentIAAnalyses, DocumentExpiration.class);

        if (extractedDates.isPresent()) {
            isCardValid = isIdentityCardValid(extractedDates.get());
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (isCardValid.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        boolean cardValid = isCardValid.orElse(false);

        if (cardValid) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFrom(getRule()),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFrom(getRule()),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
