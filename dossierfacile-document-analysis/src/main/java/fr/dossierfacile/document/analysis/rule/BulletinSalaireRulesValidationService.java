package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipClassificationValidatorB;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipContinuityRule;
import fr.dossierfacile.document.analysis.rule.validator.payslip.PayslipNamesRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulletinSalaireRulesValidationService extends AbstractRulesValidationService {

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        if (document.getDocumentCategoryStep() == DocumentCategoryStep.SALARY_EMPLOYED_NOT_YET) {
            return List.of();
        }

        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new PayslipClassificationValidatorB(),
                new PayslipNamesRule(),
                new PayslipContinuityRule()
        );
    }
}