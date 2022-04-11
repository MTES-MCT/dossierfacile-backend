package fr.gouv.bo.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantModel {
    private Long id;
    private String firstName;
    private String lastName;
    private String zipCode;
    private String email;
    private TenantType tenantType;
    private TenantFileStatus status;
    private boolean honorDeclaration;
    private LocalDateTime lastUpdateDate;
    private String clarification;
    private ApartmentSharingModel apartmentSharing;
    private List<DocumentModel> documents;
    private List<GuarantorModel> guarantors;
}
