package fr.dossierfacile.common.model.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.AuthenticityStatus;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentModel {
    private Long id;
    private DocumentCategory documentCategory;
    // TODO use DocumentSubCategory field
    @Deprecated
    private DocumentSubCategory subCategory;
    private DocumentSubCategory documentSubCategory;
    private String customText;
    private Integer monthlySum;
    private DocumentStatus documentStatus;
    private String name;
    private AuthenticityStatus authenticityStatus;
}
