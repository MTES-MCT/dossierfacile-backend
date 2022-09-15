package fr.dossierfacile.api.front.register.form.tenant;


import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.UniqueCoTenantsEmail;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.CheckCoTenantCount;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.CheckTenantTypeAcceptAccess;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DeniedJoinTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DistinctCoTenantEmailList;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.DistinctCoTenantFullNameList;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckCoTenantCount
@CheckTenantTypeAcceptAccess
@DeniedJoinTenant
@UniqueCoTenantsEmail
public class ApplicationFormV2 {

    @Null(groups = Dossier.class)
    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @NotNull
    private ApplicationType applicationType;

    @DistinctCoTenantEmailList
    @DistinctCoTenantFullNameList
    @Valid
    private List<CoTenantForm> coTenants = new ArrayList<>();

    private Boolean acceptAccess;
}
