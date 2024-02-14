package fr.dossierfacile.api.front.register.form.tenant;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountForm {

    @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    @UniqueEmailActiveAccount
    private String email;

    @NotBlank
    private String password;

    private String source;

    private String firstName;

    private String lastName;

    private String preferredName;

    private String internalPartnerId;
}
