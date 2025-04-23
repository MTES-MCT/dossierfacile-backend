package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.ListToJsonConverter;
import fr.dossierfacile.common.converter.ParsedFileConverter;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@Builder
@Entity
@Table(name = "blurry_file_analysis")
@AllArgsConstructor
@NoArgsConstructor
public class BlurryFileAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    private BlurryFileAnalysisStatus analysisStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = ListToJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<BlurryResult> blurryResults;

}
