package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.ClassificationValidatorB;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card.FrenchIdentityCardExpirationRule;
import fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card.FrenchIdentityCardNameMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.FRENCH_IDENTITY_CARD;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarteNationalIdentiteRulesValidationService extends AbstractRulesValidationService {

    private static final String DOCUMENT_IA_DOCUMENT_TYPE = "cni";

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentSubCategory() == FRENCH_IDENTITY_CARD;
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ClassificationValidatorB(DOCUMENT_IA_DOCUMENT_TYPE),
                new FrenchIdentityCardNameMatch(),
                new FrenchIdentityCardExpirationRule()
        );
    }
}