package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.recaptcha.ValidReCaptcha;
import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountForm {
    @Email
    @UniqueEmailActiveAccount
    private String email;

    @NotBlank
    private String password;

    @NotEmpty
    @ValidReCaptcha
    private String reCaptchaResponse;

    private String source;
    private String firstName;
    private String lastName;
    private String internalPartnerId;
}
