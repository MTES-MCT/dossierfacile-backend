package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "document_ia_file_analysis")
@AllArgsConstructor
@NoArgsConstructor
public class DocumentIAFileAnalysis implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251808L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Column(name = "document_ia_workflow_id", nullable = false)
    private String documentIaWorkflowId;

    @Column(name = "document_ia_execution_id")
    private String documentIaExecutionId;

    @Enumerated(EnumType.STRING)
    private DocumentIAFileAnalysisStatus analysisStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private ResultModel result;

    @Column(name = "data_file_id")
    private Long dataFileId;

    @Column(name = "data_document_id")
    private Long dataDocumentId;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @Override
    public String toString() {
        return "DocumentIAFileAnalysis{" +
                "id=" + id +
                ", file=" + (file != null ? file.getId() : null) +
                ", documentIaWorkflowId='" + documentIaWorkflowId + '\'' +
                ", documentIaExecutionId='" + documentIaExecutionId + '\'' +
                ", analysisStatus=" + analysisStatus +
                ", result=" + result +
                '}';
    }
}
