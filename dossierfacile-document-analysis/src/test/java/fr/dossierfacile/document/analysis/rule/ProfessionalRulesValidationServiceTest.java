package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.professional.Professional2DDocIssueDateRule;
import fr.dossierfacile.document.analysis.rule.validator.professional.ProfessionalClassificationRuleBI;
import fr.dossierfacile.document.analysis.rule.validator.professional.ProfessionalNamesRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalRulesValidationServiceTest {

    private final ProfessionalRulesValidationService service = new ProfessionalRulesValidationService();

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class, names = {"CDI", "CDD", "ALTERNATION", "INTERNSHIP", "INTERMITTENT"})
    void should_return_header_validators_for_professional_subcategories(DocumentSubCategory subCategory) {
        Document document = Document.builder()
                .documentSubCategory(subCategory)
                .build();

        List<AbstractDocumentRuleValidator> validators = service.getDocumentRuleValidators(document);

        assertThat(validators).hasExactlyElementsOfTypes(
                HasBeenDocumentIAAnalysedBI.class,
                ProfessionalClassificationRuleBI.class,
                Professional2DDocIssueDateRule.class,
                ProfessionalNamesRule.class
        );
    }
}
