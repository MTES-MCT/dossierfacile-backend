package fr.dossierfacile.api.front.register.form.tenant;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckBeneficiaryEmail;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckFranceConnect;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import fr.dossierfacile.common.enums.TenantOwnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckFranceConnect(groups = Dossier.class)
@CheckBeneficiaryEmail(groups = Dossier.class)
public class NamesForm implements FormWithTenantId {
    @NotNull
    private Long tenantId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String preferredName;
    private String zipCode;
    private Boolean abroad;
    private TenantOwnerType ownerType = TenantOwnerType.SELF;
    // Email of the beneficiary, only expected when ownerType is THIRD_PARTY
    @JsonDeserialize(using = EmailDeserializer.class)
    @Email
    private String beneficiaryEmail;
}
