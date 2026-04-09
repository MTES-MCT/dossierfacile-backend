package fr.dossierfacile.common.entity.rule;

import java.util.List;

public record PayslipNamesRuleData(Name expectedName, List<String> extractedIdentities) implements RuleData {
   public PayslipNamesRuleData(PayslipNamesRuleData other, List<String> extractedIdentities) {
      this(other.expectedName, List.copyOf(extractedIdentities));
   }

   public String getType() {
      return R_PAYSLIP_NAMES;
   }

   public static record Name(String firstNames, String lastName, String preferredName) {
   }
}
