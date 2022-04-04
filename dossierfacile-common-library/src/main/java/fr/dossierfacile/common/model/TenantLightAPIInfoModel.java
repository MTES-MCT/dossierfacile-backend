package fr.dossierfacile.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "All details about one tenant when apartment sharing is validated")
public class TenantLightAPIInfoModel {

    @ApiModelProperty(notes = "Email")
    private String email;
    @ApiModelProperty(notes = "Salary")
    private Integer salary;
    @ApiModelProperty(notes = "Professional situation")
    private String tenantSituation;
    @ApiModelProperty(notes = "If tenant has or not guarantors")
    private boolean guarantor;
    @Builder.Default
    @ApiModelProperty(notes = "File list")
    private Map<String, String> listFiles = new HashMap<>();
    @ApiModelProperty(notes = "List of all internalPartnerIds")
    private List<String> allInternalPartnerId;

}
