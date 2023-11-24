package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBrokenRule {

    @Enumerated(EnumType.STRING)
    private DocumentRule rule;

    private String message;

}