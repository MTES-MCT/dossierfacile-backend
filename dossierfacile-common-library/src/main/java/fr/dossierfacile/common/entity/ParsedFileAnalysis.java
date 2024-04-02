package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.ParsedFileConverter;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
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
import org.hibernate.annotations.ColumnTransformer;

@Data
@Builder
@Entity
@Table(name = "parsed_file_analysis")
@AllArgsConstructor
@NoArgsConstructor
public class ParsedFileAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    private ParsedFileAnalysisStatus analysisStatus;

    @Enumerated(EnumType.STRING)
    private ParsedFileClassification classification;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ParsedFileConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private ParsedFile parsedFile;

}
