package fr.dossierfacile.api.front.model.dfc.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;

import fr.dossierfacile.api.front.model.BaseApartmentSharingModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApartmentSharingModel extends BaseApartmentSharingModel {
    private List<TenantModel> tenants;
}
