package fr.gouv.bo.model.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
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
    private String dossierPdfUrl;
    private List<TenantModel> tenants;
}
