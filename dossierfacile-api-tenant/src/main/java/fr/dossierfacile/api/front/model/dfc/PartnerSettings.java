package fr.dossierfacile.api.front.model.dfc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PartnerSettings {
    private String name;
    private Integer version;
    @JsonProperty("callbackUrl")
    private String urlCallback;
    @JsonProperty("callbackApiKey")
    private String partnerApiKeyCallback;
}