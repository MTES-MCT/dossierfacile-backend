package fr.dossierfacile.common.entity.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FrenchIdentityCardExpirationRuleData.class, name = RuleData.R_FRENCH_IDENTITY_CARD_EXPIRATION),
        @JsonSubTypes.Type(value = TaxClassificationRuleData.class, name = RuleData.R_TAX_CLASSIFICATION),
        @JsonSubTypes.Type(value = NamesRuleData.class, name = RuleData.R_NAMES),
        @JsonSubTypes.Type(value = TaxYearsRuleData.class, name = RuleData.R_TAX_YEARS),
        @JsonSubTypes.Type(value = PayslipContinuityRuleData.class, name = RuleData.R_PAYSLIP_CONTINUITY),
})
public sealed interface RuleData permits
        FrenchIdentityCardExpirationRuleData,
        TaxClassificationRuleData,
        NamesRuleData,
        TaxYearsRuleData,
        PayslipContinuityRuleData {
    String R_FRENCH_IDENTITY_CARD_EXPIRATION = "R_FRENCH_IDENTITY_CARD_EXPIRATION";
    String R_TAX_CLASSIFICATION = "R_TAX_CLASSIFICATION";
    String R_TAX_YEARS = "R_TAX_YEARS";
    String R_NAMES = "R_NAMES";
    String R_PAYSLIP_CONTINUITY = "R_PAYSLIP_CONTINUITY";

    String getType();
}
