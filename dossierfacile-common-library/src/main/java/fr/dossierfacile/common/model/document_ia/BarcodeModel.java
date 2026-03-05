package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BarcodeModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private BarcodePosition position;
    @JsonProperty("page_number")
    private int pageNumber;
    private String type;
    @Nullable
    @JsonProperty("is_valid")
    private Boolean isValid = null;
    @JsonProperty("raw_data")
    private Object rawData;
    @JsonProperty("typed_data")
    private List<GenericProperty> typedData;
    @JsonProperty("ants_type")
    private String antsType;
}
