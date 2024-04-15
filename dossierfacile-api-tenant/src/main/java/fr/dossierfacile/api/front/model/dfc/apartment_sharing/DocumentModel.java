package fr.dossierfacile.api.front.model.dfc.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentModel {
    private Long id;
    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    private Boolean noDocument;
    private String customText;
    private Integer monthlySum;
    private DocumentStatus documentStatus;
    private DocumentDeniedReasonsModel documentDeniedReasons;
    private DocumentAnalysisReport documentAnalysisReport;

    private String name;
}
