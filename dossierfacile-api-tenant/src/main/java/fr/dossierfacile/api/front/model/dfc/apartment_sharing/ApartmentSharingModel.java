package fr.dossierfacile.api.front.model.dfc.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApartmentSharingModel {
    private Long id;
    private ApplicationType applicationType;
    private String token;
    private String tokenPublic;
    private String dossierUrl;
    private String dossierPdfUrl;
    private TenantFileStatus status;
    private List<TenantModel> tenants;
}
