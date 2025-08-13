package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@Entity
@Table(name = "blurry_file_analysis")
@AllArgsConstructor
@NoArgsConstructor
public class BlurryFileAnalysis implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251807L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    private BlurryFileAnalysisStatus analysisStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private BlurryResult blurryResults;

    private Long dataFileId;

    public String toString() {
        return "BlurryFileAnalysis{" +
                "id=" + id +
                ", file=" + (file != null ? file.getId() : null) +
                ", analysisStatus=" + analysisStatus +
                ", blurryResults=" + blurryResults +
                '}';
    }
}
