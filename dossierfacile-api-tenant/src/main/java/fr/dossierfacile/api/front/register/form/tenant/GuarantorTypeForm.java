package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.annotation.tenant.type_guarantor.MaxGuarantor;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@MaxGuarantor
public class GuarantorTypeForm implements FormWithTenantId {

    private Long tenantId;

    @NotNull
    private TypeGuarantor typeGuarantor;
}
