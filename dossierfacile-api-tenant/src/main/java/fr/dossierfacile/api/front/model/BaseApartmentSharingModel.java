package fr.dossierfacile.api.front.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseApartmentSharingModel {
    private Long id;
    private ApplicationType applicationType;
    private String token;
    private String tokenPublic;
    private String dossierUrl;
    private String dossierPdfUrl;
    private TenantFileStatus status;
}
