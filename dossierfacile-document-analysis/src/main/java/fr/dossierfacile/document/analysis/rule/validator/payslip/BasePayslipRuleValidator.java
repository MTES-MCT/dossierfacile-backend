package fr.dossierfacile.document.analysis.rule.validator.payslip;


import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public abstract class BasePayslipRuleValidator extends BaseDocumentIAValidator {

    private final Clock clock;

    protected BasePayslipRuleValidator(Clock clock) {
        this.clock = clock;
    }

    protected List<YearMonth> getExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now(clock);
        YearMonth yearMonth = YearMonth.now(clock);
        // If before the 15th, we expect M - 2, M - 3, M - 4
        // Else we expect M - 1, M - 2, M -3
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)) :
                List.of(yearMonth, yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3));
    }

    protected String getFileName(DocumentIAFileAnalysis analysis) {
        if (analysis.getFile() == null || analysis.getFile().getStorageFile() == null) {
            return null;
        }
        return analysis.getFile().getStorageFile().getName();
    }

    protected Long getFileId(DocumentIAFileAnalysis analysis) {
        if (analysis.getFile() == null) {
            return null;
        }
        return analysis.getFile().getId();
    }

}
