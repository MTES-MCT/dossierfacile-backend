package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface PartnerAccessService {

    List<PartnerAccessModel> getExternalPartners(Tenant tenant);

    void deleteAccess(Tenant tenant, Long userApiId);

}
