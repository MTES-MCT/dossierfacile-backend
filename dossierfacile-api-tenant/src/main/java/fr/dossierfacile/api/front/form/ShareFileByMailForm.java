package fr.dossierfacile.api.front.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

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
