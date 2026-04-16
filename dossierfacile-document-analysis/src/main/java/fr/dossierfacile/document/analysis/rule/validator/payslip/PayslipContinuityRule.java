package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.PayslipContinuityRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMultiMapper;
import fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model.PayslipDate;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PayslipContinuityRule extends BasePayslipRuleValidator {

    public PayslipContinuityRule() {
        super(Clock.systemDefaultZone());
    }

    public PayslipContinuityRule(Clock clock) {
        super(clock);
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

        if (document.getFiles().size() < 3) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (documentIAAnalyses.isEmpty() || hasAnyNonSuccessfulDocumentIAAnalyses(document)) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var expectedMonths = getExpectedMonthsLists();
        var continuityRuleData = new PayslipContinuityRuleData(expectedMonths, new ArrayList<>(), List.of());
        DocumentIAMultiMapper mapper = new DocumentIAMultiMapper();
        List<YearMonth> validExtractedMonths = new ArrayList<>();
        for (DocumentIAFileAnalysis analysis : documentIAAnalyses) {
            PayslipDate payslipDate = mapper.map(List.of(analysis), PayslipDate.class)
                    .stream()
                    .findFirst()
                    .orElse(null);

            YearMonth extractedMonth = null;
            if (payslipDate != null && payslipDate.startDate != null) {
                extractedMonth = YearMonth.from(payslipDate.startDate);
            }

            if (extractedMonth == null || !expectedMonths.contains(extractedMonth)) {
                continuityRuleData = continuityRuleData.addItem(
                        new PayslipContinuityRuleData.PayslipContinuityEntry(
                                getFileId(analysis),
                                getFileName(analysis),
                                extractedMonth
                        )
                );
                continue;
            }

            validExtractedMonths.add(extractedMonth);
        }

        List<YearMonth> distinctValidMonths = validExtractedMonths.stream().distinct().toList();
        List<YearMonth> missingMonthList = computeMissingMonths(expectedMonths, distinctValidMonths);
        continuityRuleData = continuityRuleData.withMissingMonthList(missingMonthList);

        // Règle métier: il faut au moins 3 mois attendus consécutifs.
        var isValid = hasThreeConsecutiveMonths(
                distinctValidMonths.stream().sorted().toList()
        );

        if (isValid) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFromWithData(
                            getRule(),
                            continuityRuleData
                    ),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFromWithData(
                            getRule(),
                            continuityRuleData
                    ),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
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

    private List<YearMonth> computeMissingMonths(List<YearMonth> expectedMonths, List<YearMonth> validExtractedMonths) {
        Set<YearMonth> extractedSet = new HashSet<>(validExtractedMonths);
        return expectedMonths.stream()
                .filter(expectedMonth -> !extractedSet.contains(expectedMonth))
                .toList();
    }
}
