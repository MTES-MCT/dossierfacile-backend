package fr.dossierfacile.common.model.ademe.housing.exit;

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
public class AdemeApiDpeExitEmissionGesJson {
    @JsonProperty("emission_ges_ch")
    private String emissionGesCh;
    @JsonProperty("emission_ges_ch_depensier")
    private String emissionGesChDepensier;
    @JsonProperty("emission_ges_ecs")
    private String emissionGesEcs;
    @JsonProperty("emission_ges_ecs_depensier")
    private String emissionGesEcsDepensier;
    @JsonProperty("emission_ges_eclairage")
    private String emissionGesEclairage;
    @JsonProperty("emission_ges_auxiliaire_generation_ch")
    private String emissionGesAuxiliaireGenerationCh;
    @JsonProperty("emission_ges_auxiliaire_generation_ch_depensier")
    private String emissionGesAuxiliaireGenerationChDepensier;
    @JsonProperty("emission_ges_auxiliaire_distribution_ch")
    private String emissionGesAuxiliaireDistributionCh;
    @JsonProperty("emission_ges_auxiliaire_generation_ecs")
    private String emissionGesAuxiliaireGenerationEcs;
    @JsonProperty("emission_ges_auxiliaire_generation_ecs_depensier")
    private String emissionGesAuxiliaireGenerationEcsDepensier;
    @JsonProperty("emission_ges_auxiliaire_distribution_ecs")
    private String emissionGesAuxiliaireDistributionEcs;
    @JsonProperty("emission_ges_auxiliaire_ventilation")
    private String emissionGesAuxiliaireVentilation;
    @JsonProperty("emission_ges_totale_auxiliaire")
    private String emissionGesTotaleAuxiliaire;
    @JsonProperty("emission_ges_fr")
    private String emissionGesFr;
    @JsonProperty("emission_ges_fr_depensier")
    private String emissionGesFrDepensier;
    @JsonProperty("emission_ges_5_usages")
    private String emissionGes5Usages;
    @JsonProperty("emission_ges_5_usages_m2")
    private String emissionGes5UsagesM2;
    @JsonProperty("classe_emission_ges")
    private String classeEmissionGes;
}
