package fr.dossierfacile.common.model.ademe.housing;

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
public class AdemeApiDpeMeteoJson {

    @JsonProperty("enum_zone_climatique_id")
    private String enumZoneClimatiqueId;
    private String altitude;
    @JsonProperty("enum_classe_altitude_id")
    private String enumClasseAltitudeId;
    @JsonProperty("batiment_materiaux_anciens")
    private String batimentMateriauxAnciens;

}
