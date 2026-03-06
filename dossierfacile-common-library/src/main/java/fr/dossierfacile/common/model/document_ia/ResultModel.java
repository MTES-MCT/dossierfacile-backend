package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ClassificationModel classification;
    private ExtractionModel extraction;
    private List<BarcodeModel> barcodes;
}
