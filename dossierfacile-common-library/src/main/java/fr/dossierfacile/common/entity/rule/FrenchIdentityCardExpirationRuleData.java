package fr.dossierfacile.common.entity.rule;

import java.time.LocalDate;

public record FrenchIdentityCardExpirationRuleData(LocalDate extractedDate) implements RuleData {
    @Override
    public String getType() {
        return R_FRENCH_IDENTITY_CARD_EXPIRATION;
    }
}
