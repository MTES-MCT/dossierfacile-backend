package fr.dossierfacile.api.front.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactForm {
    String firstname;
    String lastname;
    @NotEmpty @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    String email;
    @NotEmpty
    Profile profile;
    @NotEmpty
    String subject;
    @NotEmpty
    String message;
    public enum Profile {
        tenant, owner
    }
}
