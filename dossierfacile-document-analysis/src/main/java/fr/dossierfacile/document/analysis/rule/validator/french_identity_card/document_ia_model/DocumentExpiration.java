package fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Setter;

import java.time.LocalDate;

@Setter
public class DocumentExpiration {

    @DocumentIAField(
            twoDDocName = "numero_document",
            extractionName = "numero_document",
            type = DocumentIAPropertyType.STRING
    )
    public String cardNumber;

    @DocumentIAField(
            twoDDocName = "date_debut_validite",
            extractionName = "date_delivrance",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate deliveryDate;

    @DocumentIAField(
            twoDDocName = "date_fin_validite",
            extractionName = "date_expiration",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate expirationDate;

    @DocumentIAField(
            twoDDocName = "date_naissance",
            extractionName = "date_naissance",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate birthDate;

    // Empty constructor needed by DocumentIA Mapper
    public DocumentExpiration() {
        // Intentionally empty: required for reflection-based instantiation by DocumentIA mapper
    }

}
