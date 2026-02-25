package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;

public interface UserService {
    TenantModel createPassword(String token, String password);

    void deleteAccount(Tenant tenant);

    Boolean deleteCoTenant(Tenant tenant, Long coTenantId);

    void linkTenantToPartner(Tenant tenant, String partner, String internalPartnerId);

    void logout(String keycloakId);

}
