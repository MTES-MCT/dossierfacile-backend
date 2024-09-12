package fr.dossierfacile.common.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditedDocument {

    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    private Long tenantId;
    private Long guarantorId;
    private Long documentId;
    private EditionType editionType;

    public static EditedDocument from(Document document, EditionType editionType) {
        return EditedDocument.builder()
                .documentCategory(document.getDocumentCategory())
                .documentSubCategory(document.getDocumentSubCategory())
                .documentId(document.getId())
                .tenantId(Optional.ofNullable(document.getTenant())
                        .map(Tenant::getId).orElse(null))
                .guarantorId(Optional.ofNullable(document.getGuarantor())
                        .map(Guarantor::getId).orElse(null))
                .editionType(editionType)
                .build();
    }

}
