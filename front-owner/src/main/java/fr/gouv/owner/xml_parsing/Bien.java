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
@JsonIgnoreProperties(value = {"AGENCE", "VENTE", "APPARTEMENT", "LOCALISATION", "DEFISCALISATION",
        "COMMENTAIRES", "ALUR", "VIAGER", "IMAGES", "PIECES", "PARKING", "MAISON", "LOCAL_PROFESSIONNEL"})
public class Bien {
    @JsonProperty("INFO_GENERALES")
    private InfoGenerales infoGenerales;

    @JsonProperty("INTITULE")
    private Intitule intitule;

    @JsonProperty("LOCATION")
    private Location location;
}
