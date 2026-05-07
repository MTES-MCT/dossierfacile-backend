package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipClassificationValidatorB;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipContinuityRule;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipNamesRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BulletinSalaireRulesValidationServiceTest {

    private final BulletinSalaireRulesValidationService service = new BulletinSalaireRulesValidationService();

    @Test
    void should_return_payslip_validators_when_step_is_salary_employed_more_3_months() {
        Document document = Document.builder()
                .documentCategoryStep(DocumentCategoryStep.SALARY_EMPLOYED_MORE_3_MONTHS)
                .build();

        List<AbstractDocumentRuleValidator> validators = service.getDocumentRuleValidators(document);

        assertThat(validators).hasExactlyElementsOfTypes(
                HasBeenDocumentIAAnalysedBI.class,
                PayslipClassificationValidatorB.class,
                PayslipNamesRule.class,
                PayslipContinuityRule.class
        );
    }

    @ParameterizedTest
    @EnumSource(value = DocumentCategoryStep.class, names = {
            "SALARY_EMPLOYED_LESS_3_MONTHS",
            "SALARY_EMPLOYED_NOT_YET",
            "SALARY_FREELANCE_AUTOENTREPRENEUR",
            "SALARY_FREELANCE_OTHER",
            "SALARY_INTERMITTENT",
            "SALARY_ARTIST_AUTHOR",
            "SALARY_UNKNOWN"
    })
    void should_return_empty_for_other_salary_steps(DocumentCategoryStep step) {
        Document document = Document.builder()
                .documentCategoryStep(step)
                .build();

        assertThat(service.getDocumentRuleValidators(document)).isEmpty();
    }
}
