package fr.dossierfacile.api.front.model.dfc.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectedTenantModel {
    private Long connectedTenantId;
    private ApartmentSharingModel apartmentSharing;
}
