package fr.dossierfacile.process.file.service.documentrules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class PayslipRuleAmountValidity extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES;
    }

    // Should not happen because we have already checked that the document is parsed
    @SuppressWarnings("DataFlowIssue")
    @Override
    protected boolean isValid(Document document) {
        List<PayslipFile> recentFiles = document.getFiles().stream()
                .map(file -> (PayslipFile) file.getParsedFileAnalysis().getParsedFile())
                .sorted(Comparator.comparing(PayslipFile::getMonth).reversed())
                .limit(3)
                .toList();

        double monthlyAverage = recentFiles.stream()
                .mapToDouble(PayslipFile::getNetTaxableIncome)
                .sum() / recentFiles.size();

        // Check percentage difference
        double diffPercentage = Math.abs((monthlyAverage - document.getMonthlySum()) / document.getMonthlySum());
        return (diffPercentage <= 0.2);

    }
}
