package fr.dossierfacile.process.file.service.documentrules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
public class PayslipRuleMonthValidity extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_PAYSLIP_MONTHS;
    }

    @Override
    protected boolean isValid(Document document) {
        if (checkMonthsValidityRule(document)) {
            return true;
        } else {
            log.info("Document is expired :{}", document.getId());
            return false;
        }
    }

    //Should not happen because we have already checked that the document is parsed
    @SuppressWarnings("DataFlowIssue")
    private boolean checkMonthsValidityRule(Document document) {
        List<List<YearMonth>> expectedMonthsList = getExpectedMonthsLists();

        List<YearMonth> presentMonths = document.getFiles().stream()
                .map(file -> ((PayslipFile) file.getParsedFileAnalysis().getParsedFile()).getMonth())
                .toList();

        return expectedMonthsList.stream().anyMatch(
                presentMonths::containsAll
        );
    }

    private List<List<YearMonth>> getExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)),
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4))) :
                List.of(
                        List.of(yearMonth, yearMonth.minusMonths(1), yearMonth.minusMonths(2)),
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)));
    }
}
