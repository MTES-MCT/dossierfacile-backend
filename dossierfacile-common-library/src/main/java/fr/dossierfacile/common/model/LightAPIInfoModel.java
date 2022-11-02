package fr.dossierfacile.common.model;

import fr.dossierfacile.common.enums.PartnerCallBackType;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LightAPIInfoModel {

    private PartnerCallBackType partnerCallBackType;
    private String url;
    private String publicUrl;
    private List<String> emails;
    private List<String> internalPartnersId;
    private List<TenantLightAPIInfoModel> tenantLightAPIInfos;
}
