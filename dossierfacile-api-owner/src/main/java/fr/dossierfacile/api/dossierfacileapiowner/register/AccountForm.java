package fr.dossierfacile.api.dossierfacileapiowner.register;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountForm {

    @JsonDeserialize(using = EmailDeserializer.class)
    @UniqueEmailActiveAccount
    private String email;

    @NotBlank
    private String password;

}
