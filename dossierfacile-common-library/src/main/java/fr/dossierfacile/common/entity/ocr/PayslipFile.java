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
public class PayslipFile implements ParsedFile {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.PAYSLIP;
    ParsedStatus status;
    String fullname;
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth month;
    Double netTaxableIncome;
    Double cumulativeNetTaxableIncome;
}