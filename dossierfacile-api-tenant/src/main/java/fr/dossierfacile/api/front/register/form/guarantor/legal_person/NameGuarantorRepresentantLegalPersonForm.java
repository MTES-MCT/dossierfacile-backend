package fr.dossierfacile.api.front.register.form.guarantor.legal_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
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
public class NameGuarantorRepresentantLegalPersonForm extends DocumentGuarantorFormAbstract{
    @NotBlank
    private String firstName;

    private TypeGuarantor typeGuarantor = TypeGuarantor.LEGAL_PERSON;
}
