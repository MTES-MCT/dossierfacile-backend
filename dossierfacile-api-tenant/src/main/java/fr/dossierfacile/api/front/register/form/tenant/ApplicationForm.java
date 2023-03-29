package fr.dossierfacile.api.front.register.form.tenant;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeAcceptAccess;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.CheckTenantTypeCountListCoTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DeniedJoinTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctEmailList;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctTenantPrincipalEmailListCoTenant;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.UniqueEmailListCoTenant;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckTenantTypeCountListCoTenant
@CheckTenantTypeAcceptAccess
@DeniedJoinTenant
@UniqueEmailListCoTenant
@DistinctTenantPrincipalEmailListCoTenant
public class ApplicationForm implements FormWithTenantId {

    @NotNull
    private Long tenantId;

    @NotNull
    private ApplicationType applicationType;

    @DistinctEmailList
    @JsonDeserialize(contentUsing = EmailDeserializer.class)
    private List<@Email String> coTenantEmail = new ArrayList<>();

    private Boolean acceptAccess;
}
