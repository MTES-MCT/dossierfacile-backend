package fr.dossierfacile.common.entity.rule;

import java.util.List;

public record TaxNamesRuleData(Name expectedName, List<String> extractedIdentities) implements RuleData {

    public TaxNamesRuleData(TaxNamesRuleData other, List<String> extractedIdentities) {
        this(other.expectedName, List.copyOf(extractedIdentities));
    }

    public record Name(String firstNames, String lastName, String preferredName) {}

    @Override
    public String getType() {
        return R_TAX_NAMES;
    }
}
