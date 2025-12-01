package fr.dossierfacile.api.front.model.documentIA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.dossierfacile.common.model.documentIA.ResultModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookModelData {
    @JsonProperty("total_processing_time_ms")
    private long totalProcessingTimeMs;

    private ResultModel result;
}
