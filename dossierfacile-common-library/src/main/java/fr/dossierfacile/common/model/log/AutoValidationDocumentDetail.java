package fr.dossierfacile.common.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.DocumentAutoValidationReason;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoValidationDocumentDetail {

    private Long documentId;
    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    private DocumentCategoryStep documentCategoryStep;
    private DocumentAutoValidationReason reason;

}
