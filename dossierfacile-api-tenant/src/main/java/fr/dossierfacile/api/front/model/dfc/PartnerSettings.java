package fr.dossierfacile.api.front.model.dfc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.dossierfacile.common.deserializer.EmailDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PartnerSettings {
    private String name;
    private Integer version;
    @Email
    @JsonDeserialize(using = EmailDeserializer.class)
    private String email;
    @JsonProperty("callbackUrl")
    private String urlCallback;
    @JsonProperty("callbackApiKey")
    private String partnerApiKeyCallback;
}