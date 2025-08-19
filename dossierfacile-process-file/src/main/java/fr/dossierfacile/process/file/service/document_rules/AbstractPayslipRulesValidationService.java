package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.payslip.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Slf4j
public abstract class AbstractPayslipRulesValidationService extends AbstractRulesValidationService {
    protected abstract ParsedFileClassification getPayslipClassification();

    protected boolean isFileWithQrcode() {
        return false;
    }

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.FINANCIAL
                && document.getDocumentSubCategory() == DocumentSubCategory.SALARY
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null
                && f.getParsedFileAnalysis().getParsedFile().getClassification() == getPayslipClassification());
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new PayslipHasBeenParsedBI(),
                new PayslipRuleCheckQRCode(isFileWithQrcode()),
                new PayslipRuleNamesMatch(),
                new PayslipRuleMonthValidity(),
                new PayslipRuleAmountValidity()
        );
    }

}
