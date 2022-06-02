package fr.dossierfacile.api.dossierfacileapiowner.register;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResetForm {
    @NotEmpty
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;
}
