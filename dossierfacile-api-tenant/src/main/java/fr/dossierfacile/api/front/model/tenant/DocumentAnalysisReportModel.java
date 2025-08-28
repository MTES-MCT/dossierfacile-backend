package fr.dossierfacile.api.front.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentAnalysisReportModel {
    private Long id;
    private DocumentAnalysisStatus analysisStatus;
    private List<DocumentAnalysisRule> failedRules;
    private List<DocumentAnalysisRule> passedRules;
    private List<DocumentAnalysisRule> inconclusiveRules;
    private LocalDateTime createdAt = LocalDateTime.now();
}
