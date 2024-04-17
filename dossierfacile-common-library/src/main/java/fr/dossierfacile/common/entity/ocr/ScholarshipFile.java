package fr.dossierfacile.common.entity.ocr;

import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.*;

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
    Integer startYear;
    Integer endYear;
    Integer annualAmount;
}