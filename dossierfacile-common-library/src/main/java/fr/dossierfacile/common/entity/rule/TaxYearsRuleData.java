package fr.dossierfacile.common.entity.rule;

import java.util.List;

public record TaxYearsRuleData(int expectedYear, List<Integer> extractedYears) implements RuleData {

    public TaxYearsRuleData(TaxYearsRuleData other, List<Integer> extractedYears) {
        this(other.expectedYear, List.copyOf(extractedYears));
    }

    @Override
    public String getType() {
        return R_TAX_YEARS;
    }
}
