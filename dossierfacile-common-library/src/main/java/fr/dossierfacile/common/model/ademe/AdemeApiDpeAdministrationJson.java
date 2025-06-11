package fr.dossierfacile.common.model.ademe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.dossierfacile.common.model.ademe.geolocalisation.AdemeApiGeolocalisationJson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiDpeAdministrationJson {
    @JsonProperty("dpe_a_remplacer")
    private String dpeARemplacer;
    @JsonProperty("reference_interne_projet")
    private String referenceInterneProjet;
    @JsonProperty("motif_remplacement")
    private String motifRemplacement;
    @JsonProperty("dpe_immeuble_associe")
    private String dpeImmeubleAssocie;
    @JsonProperty("enum_version_id")
    private String enumVersionId;
    @JsonProperty("date_visite_diagnostiqueur")
    private String dateVisiteDiagnostiqueur;
    @JsonProperty("date_etablissement_dpe")
    private String dateEtablissementDpe;
    @JsonProperty("enum_modele_dpe_id")
    private String enumModeleDpeId;
    private AdemeApiDiagnosticianJson diagnostiqueur;

    private AdemeApiGeolocalisationJson geolocalisation;
}
