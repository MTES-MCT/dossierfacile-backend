package fr.dossierfacile.common.model.ademe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.dossierfacile.common.model.ademe.housing.AdemeApiDpeHousingJson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiDpeJson {

    @JsonProperty("numero_dpe")
    private String numeroDpe;
    private String statut;
    private AdemeApiDpeAdministrationJson administratif;
    private AdemeApiDpeHousingJson logement;
    @JsonProperty("logement_neuf")
    private AdemeApiDpeHousingJson logementNeuf;
}