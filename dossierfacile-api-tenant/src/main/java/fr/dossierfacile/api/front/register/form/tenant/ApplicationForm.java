package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeAcceptAccess;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeCountListCoTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DeniedJoinTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctEmailList;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctTenantPrincipalEmailListCoTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.UniqueEmailListCoTenant;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckTenantTypeCountListCoTenant
@CheckTenantTypeAcceptAccess
@DeniedJoinTenant
public class ApplicationForm {

    @NotNull
    private ApplicationType applicationType;

    @DistinctEmailList
    @UniqueEmailListCoTenant
    @DistinctTenantPrincipalEmailListCoTenant
    private List<@Email String> coTenantEmail = new ArrayList<>();

    private Boolean acceptAccess;
}
