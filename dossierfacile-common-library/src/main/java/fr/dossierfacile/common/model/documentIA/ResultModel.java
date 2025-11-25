package fr.dossierfacile.common.model.documentIA;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultModel {
    private ClassificationModel classification;
    private ExtractionModel extraction;
    private List<BarcodeModel> barcodes;
}
