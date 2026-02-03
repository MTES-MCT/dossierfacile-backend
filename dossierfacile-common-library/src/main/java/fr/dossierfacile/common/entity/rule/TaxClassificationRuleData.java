package fr.dossierfacile.common.entity.rule;

public record TaxClassificationRuleData(boolean isDeclarativeSituation) implements RuleData {
    @Override
    public String getType() {
        return R_TAX_CLASSIFICATION;
    }
}
