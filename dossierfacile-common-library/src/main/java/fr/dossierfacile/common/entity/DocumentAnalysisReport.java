package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.ListToJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Document document;

    @Enumerated(EnumType.STRING)
    private DocumentAnalysisStatus analysisStatus;

    @Convert(converter = ListToJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<DocumentBrokenRule> brokenRules;

}
