package fr.dossierfacile.common.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.AutoValidationResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoValidationLogDetails {

    private AutoValidationResultStatus status;
    private List<AutoValidationDocumentDetail> documents;

}
