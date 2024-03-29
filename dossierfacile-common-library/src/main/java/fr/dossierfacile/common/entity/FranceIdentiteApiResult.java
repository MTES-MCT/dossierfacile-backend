package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FranceIdentiteApiResult implements ParsedFile {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.FRANCE_IDENTITE;
    ParsedStatus parsedStatus;
    String status;
    FranceIdentiteApiResultAttributes attributes;
}
