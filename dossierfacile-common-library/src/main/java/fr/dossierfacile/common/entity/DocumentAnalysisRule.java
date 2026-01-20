package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.model.documentIA.GenericProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    private List<GenericProperty> expectedDatas;

    private List<GenericProperty> extractedDatas;


    public static DocumentAnalysisRule documentFailedRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getFailedMessage())
                .expectedDatas(List.of())
                .extractedDatas(List.of())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentFailedRuleFromWithData(DocumentRule rule, List<GenericProperty> expectedDatas, List<GenericProperty> extractedDatas) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getPassedMessage())
                .expectedDatas(expectedDatas)
                .extractedDatas(extractedDatas)
                .level(rule.getLevel())
                .build();
    }


    public static DocumentAnalysisRule documentPassedRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getPassedMessage())
                .expectedDatas(List.of())
                .extractedDatas(List.of())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentPassedRuleFromWithData(DocumentRule rule, List<GenericProperty> expectedDatas, List<GenericProperty> extractedDatas) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getPassedMessage())
                .expectedDatas(expectedDatas)
                .extractedDatas(extractedDatas)
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentInconclusiveRuleFrom(DocumentRule rule) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getInconclusiveMessage())
                .expectedDatas(List.of())
                .extractedDatas(List.of())
                .level(rule.getLevel())
                .build();
    }

    public static DocumentAnalysisRule documentInconclusiveRuleFromWithData(DocumentRule rule, List<GenericProperty> expectedDatas) {
        return DocumentAnalysisRule.builder()
                .rule(rule)
                .message(rule.getInconclusiveMessage())
                .expectedDatas(expectedDatas)
                .extractedDatas(List.of())
                .level(rule.getLevel())
                .build();
    }

}