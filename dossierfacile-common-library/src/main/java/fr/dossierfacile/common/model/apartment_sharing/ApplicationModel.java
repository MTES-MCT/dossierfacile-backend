package fr.dossierfacile.common.model.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationModel {
    private Long id;
    private ApplicationType applicationType;
    private PartnerCallBackType partnerCallBackType;
    private String dossierPdfUrl;
    private TenantFileStatus status;
    private List<TenantModel> tenants;
}
