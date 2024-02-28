package fr.dossierfacile.api.dossierfacileapiowner.register;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
