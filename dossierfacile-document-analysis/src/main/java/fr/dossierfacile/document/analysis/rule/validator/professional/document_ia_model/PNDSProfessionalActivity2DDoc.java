package fr.dossierfacile.document.analysis.rule.validator.professional.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.time.LocalDate;

// This model is used to sanitize the 2DDoc content of a Professional document (PNDS)
@Setter
@DocumentIAModel(documentCategory = DocumentCategory.PROFESSIONAL)
public class PNDSProfessionalActivity2DDoc {

    @DocumentIAField(twoDDocName = "doc_type")
    public String docType;

    @DocumentIAField(twoDDocName = "date_debut_contrat")
    public LocalDate dateDebutContrat;

    @DocumentIAField(twoDDocName = "periode_declaration_contrat")
    public String periodeDeclarationContrat;

    @DocumentIAField(twoDDocName = "liste_prenoms")
    public String listePrenoms;

    @DocumentIAField(twoDDocName = "nom_patronymique")
    public String nomPatronymique;

    @DocumentIAField(twoDDocName = "nature_contrat")
    public String natureContrat;

    public PNDSProfessionalActivity2DDoc() {
        // Intentionally empty: required for reflection-based instantiation by DocumentIA mapper
    }
}
