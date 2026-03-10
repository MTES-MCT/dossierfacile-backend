package fr.dossierfacile.document.analysis.rule.validator.visale_certificate.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE, documentSubCategory = DocumentSubCategory.VISALE)
public class VisaleCertificate {

    @DocumentIAField(
            extractionName = "date_delivrance"
    )
    public LocalDate dateDelivrance;

    @DocumentIAField(
            extractionName = "date_fin_validite"
    )
    public LocalDate dateFinValidite;

    @DocumentIAField(
            extractionName = "beneficiaires"
    )
    public List<VisaleBeneficiaire> beneficiaires;

    public VisaleCertificate() {
        // Required for reflection-based instantiation by DocumentIA mapper
        this.beneficiaires = List.of();
    }

    public boolean hasValidBeneficiaires() {
        return beneficiaires != null && !beneficiaires.isEmpty()
                && beneficiaires.stream().anyMatch(VisaleBeneficiaire::isValid);
    }

    public boolean hasValidExpirationDate() {
        return dateFinValidite != null;
    }
}
