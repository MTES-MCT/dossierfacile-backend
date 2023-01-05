package fr.dossierfacile.api.front.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoTenantModel {
    private Long id;
    private String firstName;
    private String lastName;
    private String preferredName;
    private String email;
    private TenantType tenantType;
    private TenantFileStatus status;
    private List<DocumentModel> documents;
    private List<GuarantorModel> guarantors;
    private boolean franceConnect;
    private Boolean allowCheckTax;
}