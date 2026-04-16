package fr.dossierfacile.common.entity.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExpirationRuleData.class, name = RuleData.R_EXPIRATION),
        @JsonSubTypes.Type(value = TaxClassificationRuleData.class, name = RuleData.R_TAX_CLASSIFICATION),
        @JsonSubTypes.Type(value = NamesRuleData.class, name = RuleData.R_NAMES),
        @JsonSubTypes.Type(value = TaxYearsRuleData.class, name = RuleData.R_TAX_YEARS),
        @JsonSubTypes.Type(value = TaxNamesRuleData.class, name = RuleData.R_TAX_NAMES),
        @JsonSubTypes.Type(value = PayslipContinuityRuleData.class, name = RuleData.R_PAYSLIP_CONTINUITY),
        @JsonSubTypes.Type(value = PayslipNamesRuleData.class, name = RuleData.R_PAYSLIP_NAMES),
        @JsonSubTypes.Type(value = PayslipClassificationRuleData.class, name = RuleData.R_PAYSLIP_CLASSIFICATION),
})
public sealed interface RuleData permits ExpirationRuleData, NamesRuleData, PayslipClassificationRuleData, PayslipContinuityRuleData, PayslipNamesRuleData, TaxClassificationRuleData, TaxNamesRuleData, TaxYearsRuleData {
    String R_EXPIRATION = "R_EXPIRATION";
    String R_TAX_CLASSIFICATION = "R_TAX_CLASSIFICATION";
    String R_TAX_YEARS = "R_TAX_YEARS";
    String R_TAX_NAMES = "R_TAX_NAMES";
    String R_NAMES = "R_NAMES";
    String R_PAYSLIP_CLASSIFICATION = "R_PAYSLIP_CLASSIFICATION";
    String R_PAYSLIP_CONTINUITY = "R_PAYSLIP_CONTINUITY";
    String R_PAYSLIP_NAMES = "R_PAYSLIP_NAMES";

    String getType();
}
