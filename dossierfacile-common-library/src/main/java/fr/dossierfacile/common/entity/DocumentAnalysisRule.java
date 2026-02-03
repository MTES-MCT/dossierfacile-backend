package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.rule.RuleData;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisRule {

    @Enumerated(EnumType.STRING)
    private DocumentRule rule;

    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentRuleLevel level = DocumentRuleLevel.CRITICAL;

    @JdbcTypeCode(SqlTypes.JSON)
    private RuleData ruleData;


    public static DocumentAnalysisRule documentFailedRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getFailedMessage())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentFailedRuleFromWithData(DocumentRule rule, RuleData ruleData) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getFailedMessage())
                .level(rule.getLevel())
                .ruleData(ruleData)
                .build();
    }


    public static DocumentAnalysisRule documentPassedRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getPassedMessage())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentPassedRuleFromWithData(DocumentRule rule, RuleData ruleData) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getPassedMessage())
                .level(rule.getLevel())
                .ruleData(ruleData)
                .build();
    }

    public static DocumentAnalysisRule documentInconclusiveRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getInconclusiveMessage())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentInconclusiveRuleFromWithData(DocumentRule rule, RuleData ruleData) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getInconclusiveMessage())
                .level(rule.getLevel())
                .ruleData(ruleData)
                .build();
    }

}