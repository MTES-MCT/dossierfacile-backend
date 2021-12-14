package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NameGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract{

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;
}
