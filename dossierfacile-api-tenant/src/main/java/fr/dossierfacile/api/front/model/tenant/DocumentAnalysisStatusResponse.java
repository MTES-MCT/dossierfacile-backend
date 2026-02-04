package fr.dossierfacile.api.front.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentAnalysisStatusResponse {
    private AnalysisStatus status;
    private Integer analyzedFiles;      // Only for IN_PROGRESS
    private Integer totalFiles;         // Only for IN_PROGRESS
    private DocumentAnalysisReportModel analysisReport; // Only for COMPLETED
}
