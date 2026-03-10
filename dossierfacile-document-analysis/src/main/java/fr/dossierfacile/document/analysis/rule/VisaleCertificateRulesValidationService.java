package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.ClassificationValidatorB;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.visale_certificate.VisaleCertificateExpirationRule;
import fr.dossierfacile.document.analysis.rule.validator.visale_certificate.VisaleCertificateNameMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaleCertificateRulesValidationService extends AbstractRulesValidationService {

    private static final String DOCUMENT_IA_DOCUMENT_TYPE = "visale_certificate";

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ClassificationValidatorB(DOCUMENT_IA_DOCUMENT_TYPE),
                new VisaleCertificateNameMatch(),
                new VisaleCertificateExpirationRule()
        );
    }
}
