package fr.dossierfacile.common.entity.rule;

import java.time.YearMonth;
import java.util.List;

public record PayslipContinuityRuleData(List<YearMonth> expectedMonthList, List<YearMonth> extractedMonthList) implements RuleData {

    public PayslipContinuityRuleData(PayslipContinuityRuleData other, List<YearMonth> extractedMonthList) {
        this(other.expectedMonthList, List.copyOf(extractedMonthList));
    }

    @Override
    public String getType() {
        return R_PAYSLIP_CONTINUITY;
    }
}
