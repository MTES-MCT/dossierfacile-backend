package fr.dossierfacile.api.dossierfacileapiowner.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionApartmentSharingOfTenantForm {
    @NotNull
    private Boolean access;
    @NotBlank
    private String kcToken;
}
