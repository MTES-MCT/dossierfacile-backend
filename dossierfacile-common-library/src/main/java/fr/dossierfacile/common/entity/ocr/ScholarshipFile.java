package fr.dossierfacile.common.entity.ocr;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.*;

import java.time.Year;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScholarshipFile implements ParsedFile {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.SCHOLARSHIP;
    ParsedStatus status;
    String firstName;
    String lastName;
    String notificationReference;
    @JsonFormat(pattern = "yyyy")
    Year startYear;
    @JsonFormat(pattern = "yyyy")
    Year endYear;
    Integer annualAmount;
}