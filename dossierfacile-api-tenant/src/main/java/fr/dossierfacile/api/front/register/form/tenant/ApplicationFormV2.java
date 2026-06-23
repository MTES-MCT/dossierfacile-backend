package fr.dossierfacile.api.front.register.form.tenant;


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
@Builder
public class ApplicationFormV2 {

    @NotNull
    private ApplicationType applicationType;

    @Valid
    private List<CoTenantForm> coTenants = new ArrayList<>();

    private Boolean acceptAccess;
}
