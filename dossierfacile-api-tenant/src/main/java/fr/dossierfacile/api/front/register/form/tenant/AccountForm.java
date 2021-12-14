package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.recaptcha.ValidReCaptcha;
import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Null;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountForm {

    @Email
    @UniqueEmailActiveAccount
    private String email;

    @Null(groups = ApiPartner.class)
    @NotBlank(groups = Dossier.class)
    private String password;

    @Null(groups = ApiPartner.class)
    @NotEmpty(groups = Dossier.class)
    @ValidReCaptcha(groups = Dossier.class)
    private String reCaptchaResponse;

    @Null(groups = ApiPartner.class)
    private String source;

    @Null(groups = ApiPartner.class)
    private String firstName;

    @Null(groups = ApiPartner.class)
    private String lastName;

    @Null(groups = ApiPartner.class)
    private String internalPartnerId;
}
