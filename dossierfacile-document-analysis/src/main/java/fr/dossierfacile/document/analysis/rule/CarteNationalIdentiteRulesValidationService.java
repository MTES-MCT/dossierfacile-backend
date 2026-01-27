package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.ClassificationValidatorB;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.FrenchIdentityCardExpirationRule;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.FrenchIdentityCardNameMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarteNationalIdentiteRulesValidationService extends AbstractRulesValidationService {

    private static final String DOCUMENT_IA_DOCUMENT_TYPE = "cni";

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ClassificationValidatorB(DOCUMENT_IA_DOCUMENT_TYPE),
                new FrenchIdentityCardNameMatch(),
                new FrenchIdentityCardExpirationRule()
        );
    }
}