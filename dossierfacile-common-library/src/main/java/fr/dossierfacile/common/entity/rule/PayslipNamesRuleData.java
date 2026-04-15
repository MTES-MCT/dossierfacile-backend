package fr.dossierfacile.common.entity.rule;

import java.util.ArrayList;
import java.util.List;

public record PayslipNamesRuleData(Name expectedName, List<PayslipNamesEntry> payslipNamesEntryList) implements RuleData {

    public PayslipNamesRuleData addItem(PayslipNamesEntry payslipNamesEntry) {
        List<PayslipNamesEntry> newList = new ArrayList<>(this.payslipNamesEntryList);
        newList.add(payslipNamesEntry);
        return new PayslipNamesRuleData(this.expectedName, newList);
    }

    public String getType() {
        return R_PAYSLIP_NAMES;
    }

    public record Name(String firstNames, String lastName, String preferredName) {
    }

    public record PayslipNamesEntry(
            Long fileId,
            String fileName,
            String ExtractedName
    ) {
    }
}
