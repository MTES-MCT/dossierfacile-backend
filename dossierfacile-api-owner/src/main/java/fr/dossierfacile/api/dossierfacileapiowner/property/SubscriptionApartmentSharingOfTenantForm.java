package fr.dossierfacile.api.dossierfacileapiowner.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionApartmentSharingOfTenantForm {
    @NotNull
    private Boolean access;
    @NotBlank
    private String kcToken;
}
