package fr.dossierfacile.document.analysis.rule.validator.visale.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Getter;

import java.time.LocalDate;

@DocumentIAModel(
        documentCategory = DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE,
        documentSubCategory = DocumentSubCategory.VISALE
)
@Getter
public class VisaleTestModel {

    @DocumentIAField(extractionName = "numero_visa", type = DocumentIAPropertyType.STRING)
    private String visaNumber;

    @DocumentIAField(extractionName = "date_delivrance", type = DocumentIAPropertyType.DATE)
    private LocalDate deliveryDate;

    @DocumentIAField(extractionName = "date_fin_validite", type = DocumentIAPropertyType.DATE)
    private LocalDate expirationDate;

}
