package fr.dossierfacile.api.front.register.form.partner;

import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountPartnerForm {

    @Email
    @UniqueEmailActiveAccount
    private String email;

    private String source;

    private String internalPartnerId;
}
