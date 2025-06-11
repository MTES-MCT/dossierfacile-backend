package fr.dossierfacile.common.model.ademe;

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
public class AdemeApiDiagnosticianJson {

    @JsonProperty("usr_logiciel_id")
    private String usrLogicielId;

    @JsonProperty("version_logiciel")
    private String versionLogiciel;

    @JsonProperty("version_moteur_calcul")
    private String versionMoteurCalcul;
}