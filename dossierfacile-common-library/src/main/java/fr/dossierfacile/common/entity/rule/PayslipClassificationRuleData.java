package fr.dossierfacile.common.entity.rule;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public record PayslipClassificationRuleData(List<PayslipClassificationEntry> entriesInError, List<YearMonth> expectedMonths) implements RuleData {

    public PayslipClassificationRuleData addItem(PayslipClassificationEntry entry) {
        List<PayslipClassificationEntry> newList = new ArrayList<>(this.entriesInError);
        newList.add(entry);
        return new PayslipClassificationRuleData(newList, expectedMonths);
    }

    @Override
    public String getType() {
        return R_PAYSLIP_CLASSIFICATION;
    }

    public record PayslipClassificationEntry(Long fileId, String fileName) {
    }
}

