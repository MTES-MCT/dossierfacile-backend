package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.tenant.type_guarantor.MaxGuarantor;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
@AllArgsConstructor
@NoArgsConstructor
@MaxGuarantor
public class GuarantorTypeForm {

    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @NotNull
    private TypeGuarantor typeGuarantor;
}
