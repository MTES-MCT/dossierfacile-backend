package fr.dossierfacile.api.front.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionApartmentSharingOfTenantForm {
    @NotNull
    private Boolean access;
}
