package fr.dossierfacile.document.analysis.rule.validator.visale_certificate.document_ia_model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.utils.IDocumentIdentity;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Setter;

import java.util.List;

@Setter
public class VisaleBeneficiaire implements IDocumentIdentity {

    @DocumentIAField(
            extractionName = "nom"
    )
    public String lastName;

    @DocumentIAField(
            extractionName = "prenoms"
    )
    public String firstName;

    public VisaleBeneficiaire() {
        // Required for reflection-based instantiation by DocumentIA mapper
    }

    public VisaleBeneficiaire(String lastName, String firstName) {
        this.lastName = lastName;
        this.firstName = firstName;
    }

    @Override
    @JsonIgnore
    public List<String> getFirstNames() {
        if (firstName != null && !firstName.isBlank()) {
            return List.of(firstName.split("\\s+"));
        }
        return List.of();
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    @JsonIgnore
    public String getPreferredName() {
        return null;
    }

    @JsonIgnore
    public boolean isValid() {
        return lastName != null && !lastName.isBlank() && firstName != null && !firstName.isBlank();
    }
}
