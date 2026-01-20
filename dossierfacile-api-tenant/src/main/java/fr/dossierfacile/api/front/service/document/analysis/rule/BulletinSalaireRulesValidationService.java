package fr.dossierfacile.api.front.service.document.analysis.rule;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.ClassificationValidatorB;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulletinSalaireRulesValidationService extends AbstractRulesValidationService {

    private static final String DOCUMENT_IA_DOCUMENT_TYPE = "bulletin_salaire";

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        if (document.getDocumentCategoryStep() == DocumentCategoryStep.SALARY_EMPLOYED_NOT_YET) {
            return List.of();
        }

        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ClassificationValidatorB(DOCUMENT_IA_DOCUMENT_TYPE)
        );
    }
}