package fr.dossierfacile.document.analysis.external.documentia;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentIARequest {
    private String metadata;
    private MultipartFile file;
    private String fileUrl;
    private Map<String, List<WorkflowV2StepParamOverride>> overrides;
}
