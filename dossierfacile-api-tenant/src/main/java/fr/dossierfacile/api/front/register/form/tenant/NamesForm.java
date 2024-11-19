package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckFranceConnect;
import fr.dossierfacile.api.front.validator.group.Dossier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckFranceConnect(groups = Dossier.class)
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
}
