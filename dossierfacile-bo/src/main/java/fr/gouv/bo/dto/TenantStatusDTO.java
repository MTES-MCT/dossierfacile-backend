package fr.gouv.bo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantStatusDTO {
    private Long id;
    private String status;
    private List<String> tenantError;
    private List<String> guarantorError;
    private String message;
    private Map<String, FileInfoDTO> files = new HashMap<>();

    public TenantStatusDTO(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public TenantStatusDTO(List<String> tenantError, List<String> guarantorError) {
        this.tenantError = tenantError;
        this.guarantorError = guarantorError;
    }

    public TenantStatusDTO(String message, Tenant tenant) {
        this.message = message;
        this.id = tenant.getId();
    }
}
