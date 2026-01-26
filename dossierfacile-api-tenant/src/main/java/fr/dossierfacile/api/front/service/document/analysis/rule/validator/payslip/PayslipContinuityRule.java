package fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMultiMapper;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip.document_ia_model.PayslipDate;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.model.documentIA.GenericProperty;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class PayslipContinuityRule extends BaseDocumentIAValidator {

    private final Clock clock;

    public PayslipContinuityRule() {
        this(Clock.systemDefaultZone());
    }

    public PayslipContinuityRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_CONTINUITY;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        if (documentIAAnalyses.isEmpty() || hasAnyNonSuccessfulDocumentIAAnalyses(document)) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var extractedDates = new DocumentIAMultiMapper().map(documentIAAnalyses, PayslipDate.class);

        if (extractedDates.stream().anyMatch(it -> it.startDate == null)) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var expectedMonths = getExpectedMonthsLists();
        var extractedMonths = getExtractedMonths(extractedDates);

        List<YearMonth> validMonths = extractedMonths.stream()
                .filter(expectedMonths::contains)
                .sorted()
                .toList();

        var isValid = hasThreeConsecutiveMonths(validMonths);

        if (isValid) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFromWithData(
                            getRule(),
                            convertYearMonthsToGenericProperties(expectedMonths),
                            convertYearMonthsToGenericProperties(extractedMonths)
                    ),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFromWithData(
                            getRule(),
                            convertYearMonthsToGenericProperties(expectedMonths),
                            convertYearMonthsToGenericProperties(extractedMonths)
                    ),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    private boolean hasThreeConsecutiveMonths(List<YearMonth> months) {
        if (months.size() < 3) {
            return false;
        }
        int consecutiveCount = 1;
        for (int i = 0; i < months.size() - 1; i++) {
            if (months.get(i).plusMonths(1).equals(months.get(i + 1))) {
                consecutiveCount++;
                if (consecutiveCount >= 3) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
            }
        }
        return false;
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }

    private List<YearMonth> getExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now(clock);
        YearMonth yearMonth = YearMonth.now(clock);
        // If before the 15th, we expect M - 2, M - 3, M - 4
        // Else we expect M - 1, M - 2, M -3
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)) :
                List.of(yearMonth, yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3));
    }

    // We need to extract YearMonth from DocumentDate based on startDate and endDate
    private List<YearMonth> getExtractedMonths(List<PayslipDate> extractedDates) {
        return extractedDates.stream()
                .map(date -> {
                    LocalDate startDate = date.startDate;
                    return YearMonth.from(startDate);
                })
                .distinct()
                .toList();
    }

    private List<GenericProperty> convertYearMonthsToGenericProperties(List<YearMonth> months) {
        return months.stream()
                .map(month -> new GenericProperty("month", month.toString(), "YearMonth"))
                .toList();
    }
}
