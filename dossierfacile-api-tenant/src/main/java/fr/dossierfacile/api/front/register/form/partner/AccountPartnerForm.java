package fr.dossierfacile.api.front.register.form.partner;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
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
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;

    private String internalPartnerId;
}
