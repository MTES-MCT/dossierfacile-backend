package fr.dossierfacile.common.entity.ocr;

import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GuaranteeProviderFile implements ParsedFile {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.GUARANTEE_PROVIDER;
    ParsedStatus status;
    List<FullName> names;
    String visaNumber;
    String deliveryDate;
    String validityDate;
    Boolean signed;

    public record FullName(String firstName, String lastName){}
}
