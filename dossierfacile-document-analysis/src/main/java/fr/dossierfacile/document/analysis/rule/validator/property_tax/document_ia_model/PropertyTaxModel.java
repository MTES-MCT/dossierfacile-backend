package fr.dossierfacile.document.analysis.rule.validator.property_tax.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

// Document-IA model for the property tax notice (taxe foncière).
@DocumentIAModel(documentCategory = DocumentCategory.RESIDENCY, documentSubCategory = DocumentSubCategory.OWNER)
public class PropertyTaxModel {

    @DocumentIAField(extractionName = "annee_imposition")
    public String anneeImposition;

    // Only the recipient identity (destinataire) is available; co-owners possibly listed on page 2
    // are not extracted for now.
    @DocumentIAField(extractionName = "proprietaire_identite")
    public String proprietaireIdentite;
}
