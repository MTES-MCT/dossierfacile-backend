package fr.dossierfacile.common.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
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
public class EditedFile {

    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    private Long tenantId;
    private Long guarantorId;
    private Long documentId;
    private Long fileId;
    private String fileName;
    private EditionType editionType;

    public static EditedFile from(File file, EditionType editionType) {
        Document document = file.getDocument();
        return EditedFile.builder()
                .documentCategory(Optional.ofNullable(document)
                        .map(Document::getDocumentCategory).orElse(null))
                .documentSubCategory(Optional.ofNullable(document)
                        .map(Document::getDocumentSubCategory).orElse(null))
                .documentId(Optional.ofNullable(document)
                        .map(Document::getId).orElse(null))
                .tenantId(Optional.ofNullable(document)
                        .map(Document::getTenant)
                        .map(Tenant::getId).orElse(null))
                .guarantorId(Optional.ofNullable(document)
                        .map(Document::getGuarantor)
                        .map(Guarantor::getId).orElse(null))
                .fileId(file.getId())
                .fileName(Optional.ofNullable(file.getStorageFile())
                        .map(StorageFile::getName).orElse(null))
                .editionType(editionType)
                .build();
    }

}
