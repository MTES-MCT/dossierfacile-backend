package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface PartnerAccessService {

    List<PartnerAccessModel> getAllPartners(Tenant tenant);

    void deleteAccess(ApartmentSharing apartmentSharing, Long userApiId);

}
