package fr.dossierfacile.api.front.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileByMailForm {
    @NotEmpty
    @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;

    @NotEmpty
    private String shareType;
}
