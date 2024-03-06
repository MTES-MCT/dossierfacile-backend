package fr.dossierfacile.common.entity.ocr;

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RentalReceiptFile implements ParsedFile {

    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.RENTAL_RECEIPT;
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth period;
    String tenantFullName;
    String ownerFullName;
    Double amount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate paymentDate;
    Boolean signed;
}
