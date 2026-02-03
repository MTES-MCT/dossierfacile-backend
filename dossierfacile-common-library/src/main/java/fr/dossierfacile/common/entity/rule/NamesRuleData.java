package fr.dossierfacile.common.entity.rule;

import java.util.List;

public record NamesRuleData(Name expectedName, List<Name> extractedNames) implements RuleData {

    public NamesRuleData(NamesRuleData other, List<Name> extractedNames) {
        this(other.expectedName, List.copyOf(extractedNames));
    }

    public record Name(String firstNames, String lastName, String preferredName) {}

    @Override
    public String getType() {
        return R_NAMES;
    }
}
