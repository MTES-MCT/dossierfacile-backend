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
public class AdemeApiDpeExitEpConsoJson {
    @JsonProperty("ep_conso_ch")
    private String epConsoCh;
    @JsonProperty("ep_conso_ch_depensier")
    private String epConsoChDepensier;
    @JsonProperty("ep_conso_ecs")
    private String epConsoEcs;
    @JsonProperty("ep_conso_ecs_depensier")
    private String epConsoEcsDepensier;
    @JsonProperty("ep_conso_eclairage")
    private String epConsoEclairage;
    @JsonProperty("ep_conso_auxiliaire_generation_ch")
    private String epConsoAuxiliaireGenerationCh;
    @JsonProperty("ep_conso_auxiliaire_generation_ch_depensier")
    private String epConsoAuxiliaireGenerationChDepensier;
    @JsonProperty("ep_conso_auxiliaire_distribution_ch")
    private String epConsoAuxiliaireDistributionCh;
    @JsonProperty("ep_conso_auxiliaire_generation_ecs")
    private String epConsoAuxiliaireGenerationEcs;
    @JsonProperty("ep_conso_auxiliaire_generation_ecs_depensier")
    private String epConsoAuxiliaireGenerationEcsDepensier;
    @JsonProperty("ep_conso_auxiliaire_distribution_ecs")
    private String epConsoAuxiliaireDistributionEcs;
    @JsonProperty("ep_conso_auxiliaire_ventilation")
    private String epConsoAuxiliaireVentilation;
    @JsonProperty("ep_conso_totale_auxiliaire")
    private String epConsoTotaleAuxiliaire;
    @JsonProperty("ep_conso_fr")
    private String epConsoFr;
    @JsonProperty("ep_conso_fr_depensier")
    private String epConsoFrDepensier;
    @JsonProperty("ep_conso_5_usages")
    private String epConso5Usages;
    @JsonProperty("ep_conso_5_usages_m2")
    private String epConso5UsagesM2;
    @JsonProperty("classe_bilan_dpe")
    private String classeBilanDpe;

}
