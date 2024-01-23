package fr.dossierfacile.common.entity.ocr;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.*;

import java.time.YearMonth;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PublicPayslipFile implements ParsedFile {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.PUBLIC_PAYSLIP;
    ParsedStatus status;
    String fullname;
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth month;
    double netTaxableIncome;
    double cumulativeNetTaxableIncome;
}