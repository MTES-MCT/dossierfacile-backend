package fr.dossierfacile.common.model.ademe.housing.exit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiDpeExitJson {
    @JsonProperty("emission_ges")
    private AdemeApiDpeExitEmissionGesJson emissionGes;
    @JsonProperty("ep_conso")
    private AdemeApiDpeExitEpConsoJson epConso;
}
