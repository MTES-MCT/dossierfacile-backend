package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.documentrules.validator.franceIdentite.FranceIdentiteHasBeenParsed;
import fr.dossierfacile.process.file.service.documentrules.validator.franceIdentite.FranceIdentiteRuleNamesMatch;
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