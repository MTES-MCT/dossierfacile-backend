package fr.dossierfacile.process.file.service.document_rules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import org.springframework.util.CollectionUtils;

public class PayslipHasBeenParsedBI extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_PARSING;
    }

    @Override
    protected boolean isValid(Document document) {

        return !CollectionUtils.isEmpty(document.getFiles()) && document.getFiles().stream()
                .noneMatch(f -> f.getParsedFileAnalysis() == null || f.getParsedFileAnalysis().getParsedFile() == null);
    }
}
