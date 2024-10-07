package fr.dossierfacile.api.front.model.dfc.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.api.front.model.tenant.DocumentModel;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantModel {
    private Long id;
    private String firstName;
    private String lastName;
    private String preferredName;
    private String zipCode;
    private String email;
    private TenantType tenantType;
    private TenantFileStatus status;
    private boolean honorDeclaration;
    private LocalDateTime lastUpdateDate;
    private LocalDateTime creationDateTime;
    private String clarification;
    private List<DocumentModel> documents;
    private List<GuarantorModel> guarantors;
    private boolean franceConnect;
}
