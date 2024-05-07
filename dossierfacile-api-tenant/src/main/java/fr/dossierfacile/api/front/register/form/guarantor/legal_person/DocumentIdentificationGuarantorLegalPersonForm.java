package fr.dossierfacile.api.front.register.form.guarantor.legal_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfPages(category = DocumentCategory.IDENTIFICATION_LEGAL_PERSON, max = 10)
public class DocumentIdentificationGuarantorLegalPersonForm extends DocumentGuarantorFormAbstract {

    @NotBlank
    private String legalPersonName;

    private TypeGuarantor typeGuarantor = TypeGuarantor.LEGAL_PERSON;

    private DocumentCategory documentCategory = DocumentCategory.IDENTIFICATION_LEGAL_PERSON;
}
