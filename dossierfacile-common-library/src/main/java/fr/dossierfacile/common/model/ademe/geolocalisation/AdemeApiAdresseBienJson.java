package fr.dossierfacile.common.model.ademe.geolocalisation;

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
public class AdemeApiAdresseBienJson {

    @JsonProperty("adresse_brut")
    private String adresseBrut;

    @JsonProperty("code_postal_brut")
    private String codePostalBrut;

    @JsonProperty("nom_commune_brut")
    private String nomCommuneBrut;

    @JsonProperty("label_brut")
    private String labelBrut;

    @JsonProperty("label_brut_avec_complement")
    private String labelBrutAvecComplement;

    @JsonProperty("enum_statut_geocodage_ban_id")
    private String enumStatutGeocodageBanId;

    @JsonProperty("ban_date_appel")
    private String banDateAppel;

    @JsonProperty("ban_id")
    private String banId;

    @JsonProperty("ban_label")
    private String banLabel;

    @JsonProperty("ban_housenumber")
    private String banHousenumber;

    @JsonProperty("ban_street")
    private String banStreet;

    @JsonProperty("ban_citycode")
    private String banCitycode;

    @JsonProperty("ban_postcode")
    private String banPostcode;

    @JsonProperty("ban_city")
    private String banCity;

    @JsonProperty("ban_type")
    private String banType;

    @JsonProperty("ban_score")
    private String banScore;

    @JsonProperty("ban_x")
    private String banX;

    @JsonProperty("ban_y")
    private String banY;

    @JsonProperty("compl_nom_residence")
    private String complNomResidence;

    @JsonProperty("compl_ref_batiment")
    private String complRefBatiment;

    @JsonProperty("compl_etage_appartement")
    private String complEtageAppartement;

    @JsonProperty("compl_ref_cage_escalier")
    private String complRefCageEscalier;

    @JsonProperty("compl_ref_logement")
    private String complRefLogement;
}