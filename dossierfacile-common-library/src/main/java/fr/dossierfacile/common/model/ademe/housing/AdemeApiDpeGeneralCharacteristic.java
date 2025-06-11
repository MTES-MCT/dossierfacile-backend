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
public class AdemeApiDpeGeneralCharacteristic {
    @JsonProperty("annee_construction")
    private String anneeConstruction;
    @JsonProperty("enum_periode_construction_id")
    private String enumPeriodeConstructionId;
    @JsonProperty("enum_methode_application_dpe_log_id")
    private String enumMethodeApplicationDpeLogId;
    @JsonProperty("surface_habitable_logement")
    private String surfaceHabitableLogement;
    @JsonProperty("nombre_niveau_logement")
    private String nombreNiveauLogement;
    private String hsp;
    @JsonProperty("nombre_appartement")
    private String nombreAppartement;
}
