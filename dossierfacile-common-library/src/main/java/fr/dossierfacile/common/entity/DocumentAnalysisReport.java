package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<DocumentBrokenRule> brokenRules;
    private String comment;

}
