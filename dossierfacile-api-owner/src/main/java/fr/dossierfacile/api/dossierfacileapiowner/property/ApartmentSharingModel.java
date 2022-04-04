package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
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
    private List<TenantModel> tenants;
    private ApplicationType applicationType;
}
