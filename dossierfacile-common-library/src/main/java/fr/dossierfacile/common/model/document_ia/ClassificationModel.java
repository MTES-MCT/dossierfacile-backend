package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassificationModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("document_type")
    private String documentType;
    private String explanation;
}
