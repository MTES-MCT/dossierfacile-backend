package fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.IDENTIFICATION, documentSubCategory = DocumentSubCategory.FRENCH_IDENTITY_CARD)
public class DocumentExpiration {

    @DocumentIAField(
            twoDDocName = "numero_document",
            extractionName = "numero_document"
    )
    public String cardNumber;

    @DocumentIAField(
            twoDDocName = "date_debut_validite",
            extractionName = "date_delivrance"
    )
    public LocalDate deliveryDate;

    @DocumentIAField(
            twoDDocName = "date_fin_validite",
            extractionName = "date_expiration"
    )
    public LocalDate expirationDate;

    @DocumentIAField(
            twoDDocName = "date_naissance",
            extractionName = "date_naissance"
    )
    public LocalDate birthDate;

    // Empty constructor needed by DocumentIA Mapper
    public DocumentExpiration() {
        // Intentionally empty: required for reflection-based instantiation by DocumentIA mapper
    }

}
