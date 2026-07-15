package fr.dossierfacile.api.front.register.form.tenant;


import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CheckCoTenantCount;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CheckTenantTypeAcceptAccess;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.CoTenantsEmailRequired;
import fr.dossierfacile.api.front.validator.annotation.tenant.application.v2.DistinctCoTenantEmailList;
import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckCoTenantCount
@CheckTenantTypeAcceptAccess
@CoTenantsEmailRequired
@Builder
public class ApplicationFormV2 {

    @NotNull
    private ApplicationType applicationType;

    @DistinctCoTenantEmailList
    @Valid
    @Builder.Default
    private List<CoTenantForm> coTenants = new ArrayList<>();

    private Boolean acceptAccess;
}
