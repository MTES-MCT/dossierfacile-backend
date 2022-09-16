package fr.dossierfacile.api.front.register.form.tenant;

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
public class CoTenantForm {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;

    private String preferredName;

    @JsonDeserialize(using = EmailDeserializer.class)
    @Email
    private String email;
}
