package fr.dossierfacile.common.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBrokenRule {

    @Enumerated(EnumType.STRING)
    private DocumentRule rule;

    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentRule.Level level = DocumentRule.Level.CRITICAL;


    public static DocumentBrokenRule of(DocumentRule rule) {
        return DocumentBrokenRule.builder()
                .rule(rule)
                .message(rule.getDefaultMessage())
                .level(rule.getLevel())
                .build();
    }

}