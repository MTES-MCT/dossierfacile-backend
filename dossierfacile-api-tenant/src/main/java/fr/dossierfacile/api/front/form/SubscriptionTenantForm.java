package fr.dossierfacile.api.front.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionTenantForm {
    @NotNull
    private Boolean access;
}
