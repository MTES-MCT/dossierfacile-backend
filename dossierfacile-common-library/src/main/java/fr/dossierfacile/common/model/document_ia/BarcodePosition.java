package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BarcodePosition {
    @JsonProperty("top_left")
    private int[] topLeft;
    @JsonProperty("top_right")
    private int[] topRight;
    @JsonProperty("bottom_left")
    private int[] bottomLeft;
    @JsonProperty("bottom_right")
    private int[] bottomRight;
}
