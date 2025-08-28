package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.converter.ListOfRulesJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "document_analysis_report")
@AllArgsConstructor
@NoArgsConstructor
public class DocumentAnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(targetEntity = Document.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonIgnore
    private Document document;
    @Enumerated(EnumType.STRING)
    private DocumentAnalysisStatus analysisStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = ListOfRulesJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<DocumentAnalysisRule> failedRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = ListOfRulesJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<DocumentAnalysisRule> passedRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = ListOfRulesJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<DocumentAnalysisRule> inconclusiveRules;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private Long dataDocumentId;

    private String comment;

    public void addDocumentFailedRule(DocumentAnalysisRule documentBrokenRule) {
        if (failedRules == null) {
            failedRules = List.of(documentBrokenRule);
        } else {
            failedRules.add(documentBrokenRule);
        }
    }

    public void addDocumentPassedRule(DocumentAnalysisRule documentPassedRule) {
        if (passedRules == null) {
            passedRules = List.of(documentPassedRule);
        } else {
            passedRules.add(documentPassedRule);
        }
    }

    public void addDocumentInconclusiveRule(DocumentAnalysisRule documentInconclusiveRule) {
        if (inconclusiveRules == null) {
            inconclusiveRules = List.of(documentInconclusiveRule);
        } else {
            inconclusiveRules.add(documentInconclusiveRule);
        }
    }

    public List<DocumentAnalysisRule> getFilteredPassedRules(DocumentRuleLevel level) {
        return passedRules.stream().filter(item -> item.getLevel().ordinal() >= level.ordinal()).toList();
    }

    public List<DocumentAnalysisRule> getFilteredFailedRules(DocumentRuleLevel level) {
        return failedRules.stream().filter(item -> item.getLevel().ordinal() >= level.ordinal()).toList();
    }

    public List<DocumentAnalysisRule> getFilteredInconclusiveRules(DocumentRuleLevel level) {
        return inconclusiveRules.stream().filter(item -> item.getLevel().ordinal() >= level.ordinal()).toList();
    }

}
