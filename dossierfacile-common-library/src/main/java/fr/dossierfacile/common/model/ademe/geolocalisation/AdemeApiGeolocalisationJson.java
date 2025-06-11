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
public class AdemeApiGeolocalisationJson {

    @JsonProperty("numero_fiscal_local")
    private String numeroFiscalLocal;

    @JsonProperty("id_batiment_rnb")
    private String idBatimentRnb;

    @JsonProperty("rpls_log_id")
    private String rplsLogId;

    @JsonProperty("rpls_org_id")
    private String rplsOrgId;

    @JsonProperty("idpar")
    private String idpar;

    @JsonProperty("immatriculation_copropriete")
    private String immatriculationCopropriete;

    private AdemeApiAdressesJson adresses;
}
