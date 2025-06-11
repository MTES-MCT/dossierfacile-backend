package fr.dossierfacile.common.model.ademe.housing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.dossierfacile.common.model.ademe.housing.exit.AdemeApiDpeExitJson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiDpeHousingJson {
    @JsonProperty("caracteristique_generale")
    private AdemeApiDpeGeneralCharacteristic caracteristiqueGenerale;
    private AdemeApiDpeMeteoJson meteo;
    private AdemeApiDpeExitJson sortie;
}
