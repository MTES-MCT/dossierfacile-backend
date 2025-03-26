package fr.dossierfacile.api.front.register.form.tenant;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoTenantForm {
    @Nullable
    private String firstName;
    @Nullable
    private String lastName;

    private String preferredName;

    @JsonDeserialize(using = EmailDeserializer.class)
    @Email
    private String email;
}
