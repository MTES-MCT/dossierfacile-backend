package fr.gouv.bo.dto;

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

    @ApiModelProperty(notes = "type of partner call back")
    private PartnerCallBackType partnerCallBackType;
    @ApiModelProperty(notes = "Url to view full apartment sharing")
    private String url;
    @ApiModelProperty(notes = "Url to view light apartment sharing")
    private String publicUrl;
    @ApiModelProperty(notes = "Tenant's emails list in shared apartment")
    private List<String> emails;
    @ApiModelProperty(notes = "Internal partners id of tenant in the apartment sharing")
    private List<String> internalPartnersId;
    @ApiModelProperty(notes = "Tenant with more info")
    private List<TenantLightAPIInfoModel> tenantLightAPIInfos;
}
