package fr.dossierfacile.api.front.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

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
    String profile;
    @NotEmpty
    String subject;
    @NotEmpty
    String message;
}
