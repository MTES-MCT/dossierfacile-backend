package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.common.entity.rule.FrenchIdentityCardExpirationRuleData;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.entity.rule.PayslipContinuityRuleData;
import fr.dossierfacile.common.entity.rule.RuleData;
import fr.dossierfacile.common.entity.rule.TaxClassificationRuleData;
import fr.dossierfacile.common.entity.rule.TaxYearsRuleData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAnalysisRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void should_serialize_and_deserialize_FrenchIdentityCardExpirationRuleData() throws Exception {
        RuleData ruleData = new FrenchIdentityCardExpirationRuleData(LocalDate.of(2025, 12, 31));
        DocumentAnalysisRule rule = DocumentAnalysisRule.builder()
                .rule(DocumentRule.R_FRENCH_IDENTITY_CARD_EXPIRATION)
                .ruleData(ruleData)
                .build();

        String json = objectMapper.writeValueAsString(rule);
        DocumentAnalysisRule deserializedRule = objectMapper.readValue(json, DocumentAnalysisRule.class);

        assertThat(deserializedRule.getRule()).isEqualTo(DocumentRule.R_FRENCH_IDENTITY_CARD_EXPIRATION);
        assertThat(deserializedRule.getRuleData()).isInstanceOf(FrenchIdentityCardExpirationRuleData.class);
        assertThat(((FrenchIdentityCardExpirationRuleData) deserializedRule.getRuleData()).extractedDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(deserializedRule.getRuleData().getType()).isEqualTo(RuleData.R_FRENCH_IDENTITY_CARD_EXPIRATION);
    }

    @Test
    void should_serialize_and_deserialize_TaxClassificationRuleData() throws Exception {
        RuleData ruleData = new TaxClassificationRuleData(true);
        DocumentAnalysisRule rule = DocumentAnalysisRule.builder()
                .rule(DocumentRule.R_TAX_BAD_CLASSIFICATION)
                .ruleData(ruleData)
                .build();

        String json = objectMapper.writeValueAsString(rule);
        DocumentAnalysisRule deserializedRule = objectMapper.readValue(json, DocumentAnalysisRule.class);

        assertThat(deserializedRule.getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
        assertThat(deserializedRule.getRuleData()).isInstanceOf(TaxClassificationRuleData.class);
        assertThat(((TaxClassificationRuleData) deserializedRule.getRuleData()).isDeclarativeSituation()).isTrue();
        assertThat(deserializedRule.getRuleData().getType()).isEqualTo(RuleData.R_TAX_CLASSIFICATION);
    }

    @Test
    void should_serialize_and_deserialize_NamesRuleData() throws Exception {
        NamesRuleData.Name expected = new NamesRuleData.Name("John", "Doe", "Johnny");
        NamesRuleData.Name extracted = new NamesRuleData.Name("John", "Doe", null);
        RuleData ruleData = new NamesRuleData(expected, List.of(extracted));

        DocumentAnalysisRule rule = DocumentAnalysisRule.builder()
                .rule(DocumentRule.R_TAX_NAMES)
                .ruleData(ruleData)
                .build();

        String json = objectMapper.writeValueAsString(rule);
        DocumentAnalysisRule deserializedRule = objectMapper.readValue(json, DocumentAnalysisRule.class);

        assertThat(deserializedRule.getRule()).isEqualTo(DocumentRule.R_TAX_NAMES);
        assertThat(deserializedRule.getRuleData()).isInstanceOf(NamesRuleData.class);
        NamesRuleData deserializedData = (NamesRuleData) deserializedRule.getRuleData();
        assertThat(deserializedData.expectedName()).isEqualTo(expected);
        assertThat(deserializedData.extractedNames()).containsExactly(extracted);
        assertThat(deserializedRule.getRuleData().getType()).isEqualTo(RuleData.R_NAMES);
    }

    @Test
    void should_serialize_and_deserialize_TaxYearsRuleData() throws Exception {
        RuleData ruleData = new TaxYearsRuleData(2023, List.of(2023, 2022));
        DocumentAnalysisRule rule = DocumentAnalysisRule.builder()
                .rule(DocumentRule.R_TAX_WRONG_YEAR)
                .ruleData(ruleData)
                .build();

        String json = objectMapper.writeValueAsString(rule);
        DocumentAnalysisRule deserializedRule = objectMapper.readValue(json, DocumentAnalysisRule.class);

        assertThat(deserializedRule.getRule()).isEqualTo(DocumentRule.R_TAX_WRONG_YEAR);
        assertThat(deserializedRule.getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData deserializedData = (TaxYearsRuleData) deserializedRule.getRuleData();
        assertThat(deserializedData.expectedYear()).isEqualTo(2023);
        assertThat(deserializedData.extractedYears()).containsExactly(2023, 2022);
        assertThat(deserializedRule.getRuleData().getType()).isEqualTo(RuleData.R_TAX_YEARS);
    }

    @Test
    void should_serialize_and_deserialize_PayslipContinuityRuleData() throws Exception {
        YearMonth ym1 = YearMonth.of(2023, 1);
        YearMonth ym2 = YearMonth.of(2023, 2);
        RuleData ruleData = new PayslipContinuityRuleData(List.of(ym1, ym2), List.of(ym1));

        DocumentAnalysisRule rule = DocumentAnalysisRule.builder()
                .rule(DocumentRule.R_PAYSLIP_CONTINUITY)
                .ruleData(ruleData)
                .build();

        String json = objectMapper.writeValueAsString(rule);
        DocumentAnalysisRule deserializedRule = objectMapper.readValue(json, DocumentAnalysisRule.class);

        assertThat(deserializedRule.getRule()).isEqualTo(DocumentRule.R_PAYSLIP_CONTINUITY);
        assertThat(deserializedRule.getRuleData()).isInstanceOf(PayslipContinuityRuleData.class);
        PayslipContinuityRuleData deserializedData = (PayslipContinuityRuleData) deserializedRule.getRuleData();
        assertThat(deserializedData.expectedMonthList()).containsExactly(ym1, ym2);
        assertThat(deserializedData.extractedMonthList()).containsExactly(ym1);
        assertThat(deserializedRule.getRuleData().getType()).isEqualTo(RuleData.R_PAYSLIP_CONTINUITY);
    }
}
