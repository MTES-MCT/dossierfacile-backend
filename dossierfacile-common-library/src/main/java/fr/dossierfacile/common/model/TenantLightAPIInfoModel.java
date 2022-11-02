package fr.dossierfacile.common.model;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TenantLightAPIInfoModel {

    private String email;
    private Integer salary;
    private String tenantSituation;
    private boolean guarantor;
    @Builder.Default
    private Map<String, String> listFiles = new HashMap<>();
    private List<String> allInternalPartnerId;

}
