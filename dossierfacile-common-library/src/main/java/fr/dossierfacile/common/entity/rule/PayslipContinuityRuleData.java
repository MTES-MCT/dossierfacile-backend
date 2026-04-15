package fr.dossierfacile.common.entity.rule;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public record PayslipContinuityRuleData(
        List<YearMonth> expectedMonthList,
        List<PayslipContinuityEntry> payslipContinuityEntries,
        List<YearMonth> missingMonthList
) implements RuleData {

    public PayslipContinuityRuleData addItem(PayslipContinuityEntry payslipContinuityEntry) {
        List<PayslipContinuityEntry> newList = new ArrayList<>(this.payslipContinuityEntries);
        newList.add(payslipContinuityEntry);
        return new PayslipContinuityRuleData(this.expectedMonthList, newList, this.missingMonthList);
    }

    public PayslipContinuityRuleData withMissingMonthList(List<YearMonth> missingMonthList) {
        return new PayslipContinuityRuleData(this.expectedMonthList, this.payslipContinuityEntries, missingMonthList);
    }

    @Override
    public String getType() {
        return R_PAYSLIP_CONTINUITY;
    }

    public record PayslipContinuityEntry(Long fileId, String fileName, YearMonth extractedMonth) {
    }
}
