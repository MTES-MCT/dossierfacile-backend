package fr.dossierfacile.process.file.service.documentrules.validator.rental;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
public class RentalRuleMonthValidity extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_RENT_RECEIPT_MONTHS;
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
        List<List<YearMonth>> expectedMonthsList = (document.getTenant() != null) ? getExpectedMonthsLists() : getGuarantorExpectedMonthsLists();

        List<YearMonth> presentMonths = document.getFiles().stream()
                .map(file -> ((RentalReceiptFile) file.getParsedFileAnalysis().getParsedFile()).getPeriod())
                .toList();

        return expectedMonthsList.stream().anyMatch(presentMonths::containsAll);
    }

    private List<List<YearMonth>> getExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)),
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)),
                        List.of(yearMonth.minusMonths(3), yearMonth.minusMonths(4), yearMonth.minusMonths(5))) :
                List.of(
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)),
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)));
    }

    private List<List<YearMonth>> getGuarantorExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(
                        List.of(yearMonth.minusMonths(1)),
                        List.of(yearMonth.minusMonths(2)),
                        List.of(yearMonth.minusMonths(3))) :
                List.of(
                        List.of(yearMonth.minusMonths(1)),
                        List.of(yearMonth.minusMonths(2)));
    }
}
