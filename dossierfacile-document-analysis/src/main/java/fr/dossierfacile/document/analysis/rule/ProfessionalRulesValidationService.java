package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.professional.Professional2DDocIssueDateRule;
import fr.dossierfacile.document.analysis.rule.validator.professional.ProfessionalClassificationRuleBI;
import fr.dossierfacile.document.analysis.rule.validator.professional.ProfessionalNamesRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfessionalRulesValidationService extends AbstractRulesValidationService {

    public static final Set<DocumentSubCategory> PROFESSIONAL_SUB_CATEGORIES = EnumSet.of(
            DocumentSubCategory.CDI,
            DocumentSubCategory.CDD,
            DocumentSubCategory.ALTERNATION,
            DocumentSubCategory.INTERNSHIP,
            DocumentSubCategory.INTERMITTENT
    );

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new ProfessionalClassificationRuleBI(),
                new Professional2DDocIssueDateRule(),
                new ProfessionalNamesRule()
        );
    }
}
