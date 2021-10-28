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
@JsonIgnoreProperties(value = {"DATE_CREATION", "DATE_MAJ", "LIBRELE", "NEGO", "AFF_URL", "VISITE_VIRTUELLE", "STATUT"})
public class InfoGenerales {
    @JsonProperty("AFF_ID")
    private String affId;
    @JsonProperty("AFF_NUM")
    private String affNum;
}
