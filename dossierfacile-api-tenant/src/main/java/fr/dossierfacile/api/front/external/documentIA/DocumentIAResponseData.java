package fr.dossierfacile.api.front.external.documentIA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentIAResponseData {
    @JsonProperty("execution_id")
    private String executionId;
    @JsonProperty("workflow_id")
    private String workflowId;
}
