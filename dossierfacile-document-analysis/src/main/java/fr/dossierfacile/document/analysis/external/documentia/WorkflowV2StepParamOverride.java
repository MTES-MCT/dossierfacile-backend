package fr.dossierfacile.document.analysis.external.documentia;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowV2StepParamOverride {
    private String param;
    private Object value; // Supports String, List, Double, Boolean
}
