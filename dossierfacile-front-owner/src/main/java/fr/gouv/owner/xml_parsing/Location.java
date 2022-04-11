package fr.gouv.owner.xml_parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {"TYPE_LOYER", "INFO_PRIX", "NUM_MANDAT", "TYPE_MANDAT", "FRAIS_AGENCE", "DATE_MANDAT",
        "LOYER_HT_HC_AN", "DEPOT_GARANTIE", "PROVISION_SUR_CHARGES", "GARANTIE_REVENTE", "HONO_ETAT_LIEUX_LOC",
        "HONO_ETAT_LIEUX_BAILLEUR", "TAXE_FONCIERE", "REGUL_CHARGES"})
public class Location {
    @JsonProperty("LOYER")
    private Double loyer;
}
