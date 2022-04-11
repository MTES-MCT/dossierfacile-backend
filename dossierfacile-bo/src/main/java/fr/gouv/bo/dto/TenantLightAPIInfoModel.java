package fr.gouv.bo.dto;

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

    @ApiModelProperty(notes = "email")
    private String email;
    @ApiModelProperty(notes = "salary")
    private Integer salary;
    @ApiModelProperty(notes = "professional situation")
    private String tenantSituation;
    @ApiModelProperty(notes = "guarantor")
    private boolean guarantor;
    @ApiModelProperty(notes = "list files")
    private Map<String, String> listFiles = new HashMap<>();
    @ApiModelProperty(notes = "list all internalPartnerIds")
    private List<String> allInternalPartnerId;

}
