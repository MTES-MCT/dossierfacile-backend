package fr.dossierfacile.api.front.register.form.tenant;


import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationFormV2 implements FormWithTenantId {

    @Null(groups = Dossier.class)
    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @NotNull
    private ApplicationType applicationType;

    @Valid
    private List<CoTenantForm> coTenants = new ArrayList<>();

    private Boolean acceptAccess;
}
