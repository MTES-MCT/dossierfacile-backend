package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.tenant.name.CheckFranceConnect;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckFranceConnect(groups = Dossier.class)
public class NamesForm {

    @Null(groups = Dossier.class)
    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String preferredName;

    private String zipCode;
}
