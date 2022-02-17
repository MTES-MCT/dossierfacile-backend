package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.type.TaxDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageItem {

    private String commentDoc;
    private Integer monthlySum;
    private String customTex;
    private TaxDocument taxDocument;
    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    @Builder.Default
    private List<ItemDetail> itemDetailList = new ArrayList<>();
    private Long documentId;
    private String documentName;
    @Builder.Default
    private List<File> files = new ArrayList<>();
}
