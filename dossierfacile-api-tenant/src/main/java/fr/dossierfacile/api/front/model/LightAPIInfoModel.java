package fr.dossierfacile.api.front.model;

import fr.dossierfacile.common.enums.PartnerCallBackType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "All details about tenants when apartment sharing is validated")
public class LightAPIInfoModel {

    @ApiModelProperty(notes = "Type of partner callback")
    private PartnerCallBackType partnerCallBackType;
    @ApiModelProperty(notes = "Url to view full information of the apartment sharing")
    private String url;
    @ApiModelProperty(notes = "Url to view limited information of the apartment sharing")
    private String publicUrl;
    @ApiModelProperty(notes = "Tenant's emails list")
    private List<String> emails;
    @ApiModelProperty(notes = "List of internal partner id of tenant")
    private List<String> internalPartnersId;
    @ApiModelProperty(notes = "More info of tenants")
    private List<TenantLightAPIInfoModel> tenantLightAPIInfos;
}
