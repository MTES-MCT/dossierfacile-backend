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
public class UpdateMonthlySum {

    private DocumentCategory documentCategory;
    private DocumentSubCategory documentSubCategory;
    private Long tenantId;
    private Long guarantorId;
    private Long documentId;
    private Integer oldSum;
    private Integer newSum;

    public static UpdateMonthlySum from(Document document, Integer newSum) {
        return UpdateMonthlySum.builder()
                .documentCategory(document.getDocumentCategory())
                .documentSubCategory(document.getDocumentSubCategory())
                .documentId(document.getId())
                .tenantId(Optional.ofNullable(document.getTenant())
                        .map(Tenant::getId).orElse(null))
                .guarantorId(Optional.ofNullable(document.getGuarantor())
                        .map(Guarantor::getId).orElse(null))
                .oldSum(document.getMonthlySum())
                .newSum(newSum)
                .build();
    }

}
