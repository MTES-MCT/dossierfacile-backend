package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentIAResultModel {
    private String id;
    private DocumentIAFileAnalysisStatus status;
    private DocumentIaResultDataModel data;
}


