package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.Data;

@Data
public class DocumentDeniedOptionsDTO {
    private DocumentSubCategory documentSubCategory;
    private DocumentCategory documentCategory;
    private String documentUserType;
    private String messageValue;
}
