package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import fr.dossierfacile.common.converter.ParsedFileConverter;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Convert;
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

@Data
@Builder
@Entity
@Table(name = "parsed_file_analysis")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
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
