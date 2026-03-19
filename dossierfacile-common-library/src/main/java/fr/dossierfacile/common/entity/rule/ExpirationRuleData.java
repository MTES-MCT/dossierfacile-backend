package fr.dossierfacile.common.entity.rule;

import java.time.LocalDate;

public record ExpirationRuleData(LocalDate extractedDate) implements RuleData {
    @Override
    public String getType() {
        return R_EXPIRATION;
    }
}
