package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantModel {
    private Long id;
    private String firstName;
    private String lastName;
    private TenantFileStatus status;
    private Date lastUpdateDate;
    private String tenantType;
}
