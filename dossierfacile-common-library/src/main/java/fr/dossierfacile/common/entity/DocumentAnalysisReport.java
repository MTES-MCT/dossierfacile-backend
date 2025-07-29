package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.converter.ListOfBrokenRulesJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Convert(converter = ListOfBrokenRulesJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<DocumentBrokenRule> brokenRules;
    private String comment;

    public void addDocumentBrokenRule(DocumentBrokenRule documentBrokenRule) {
        if (brokenRules == null) {
            brokenRules = List.of(documentBrokenRule);
        } else {
            brokenRules.add(documentBrokenRule);
        }
    }

}
