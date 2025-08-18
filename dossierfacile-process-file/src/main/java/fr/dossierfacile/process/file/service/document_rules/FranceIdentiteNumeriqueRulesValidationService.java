package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.france_identite.FranceIdentiteHasBeenParsed;
import fr.dossierfacile.process.file.service.document_rules.validator.france_identite.FranceIdentiteRuleNamesMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.FRANCE_IDENTITE;

@Service
@RequiredArgsConstructor
@Slf4j
public class FranceIdentiteNumeriqueRulesValidationService extends AbstractRulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentSubCategory() == FRANCE_IDENTITE;
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new FranceIdentiteHasBeenParsed(),
                new FranceIdentiteRuleNamesMatch()
        );
    }
}