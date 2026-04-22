package fr.dossierfacile.common.entity.rule;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public record PayslipContinuityRuleData(
        List<YearMonth> expectedMonthList,
        List<PayslipContinuityEntry> payslipEntriesInError,
        List<YearMonth> missingMonthList
) implements RuleData {

    public PayslipContinuityRuleData addItem(PayslipContinuityEntry errorEntry) {
        List<PayslipContinuityEntry> newList = new ArrayList<>(this.payslipEntriesInError);
        newList.add(errorEntry);
        return new PayslipContinuityRuleData(this.expectedMonthList, newList, this.missingMonthList);
    }

    public PayslipContinuityRuleData withMissingMonthList(List<YearMonth> missingMonthList) {
        return new PayslipContinuityRuleData(this.expectedMonthList, this.payslipEntriesInError, missingMonthList);
    }

    @Override
    public String getType() {
        return R_PAYSLIP_CONTINUITY;
    }

    public record PayslipContinuityEntry(Long fileId, String fileName, YearMonth extractedMonth) {
    }
}
