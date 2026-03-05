package fr.dossierfacile.document.analysis.rule.validator.visale_certificate;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.VisaleCertificateExpirationRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.visale_certificate.document_ia_model.VisaleCertificate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Slf4j
public class VisaleCertificateExpirationRule extends BaseDocumentIAValidator {

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
        return DocumentRule.R_VISALE_CERTIFICATE_EXPIRATION;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), null),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        var extractedCertificate = new DocumentIAMergerMapper().map(documentIAAnalyses, VisaleCertificate.class);

        if (extractedCertificate.isEmpty() || !extractedCertificate.get().hasValidExpirationDate()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), null),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        VisaleCertificate certificate = extractedCertificate.get();
        LocalDate expirationDate = certificate.dateFinValidite;

        VisaleCertificateExpirationRuleData ruleData = new VisaleCertificateExpirationRuleData(expirationDate);

        boolean isValid = isExpirationDateValid(expirationDate);

        if (isValid) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), ruleData),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    /**
     * Checks if the expiration date is still valid (i.e., in the future or today).
     */
    private boolean isExpirationDateValid(LocalDate expirationDate) {
        return !expirationDate.isBefore(LocalDate.now());
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
