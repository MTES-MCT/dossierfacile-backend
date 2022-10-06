package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.Data;

@Data
public class DocumentDeniedOptionsDTO {
    private DocumentSubCategory documentSubCategory;
    private String documentUserType;
    private String messageValue;
}
