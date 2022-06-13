package fr.dossierfacile.api.front.register.form.partner;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailExistsForm {

    @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;
}
