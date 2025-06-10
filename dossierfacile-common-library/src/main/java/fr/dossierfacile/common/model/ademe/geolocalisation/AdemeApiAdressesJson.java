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
public class AdemeApiAdressesJson {

    @JsonProperty("adresse_bien")
    private AdemeApiAdresseBienJson adresseBien;
}