package fr.dossierfacile.document.analysis.rule.validator.property_tax.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

import java.util.List;

// Document-IA model for the property tax notice (taxe foncière).
@DocumentIAModel(documentCategory = DocumentCategory.RESIDENCY, documentSubCategory = DocumentSubCategory.OWNER)
public class PropertyTaxModel {

    @DocumentIAField(extractionName = "annee_imposition")
    public String anneeImposition;

    // Owners of the property
    @DocumentIAField(extractionName = "identites_proprietaires")
    public List<String> identitesProprietaires;

    // Recipients of the notice
    @DocumentIAField(extractionName = "identite_destinataire")
    public List<String> identiteDestinataire;
}
