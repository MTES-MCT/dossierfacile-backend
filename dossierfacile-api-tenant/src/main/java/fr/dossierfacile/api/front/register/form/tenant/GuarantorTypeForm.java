package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.tenant.type_guarantor.MaxGuarantor;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuarantorTypeForm {
    @NotNull
    @MaxGuarantor
    private TypeGuarantor typeGuarantor;
}
