package fr.dossierfacile.api.dossierfacileapiowner.user;

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
public class NamesForm {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;
}
