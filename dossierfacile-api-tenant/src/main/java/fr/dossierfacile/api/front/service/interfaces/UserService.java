package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;

public interface UserService {
    long confirmAccount(String token);

    TenantModel createPassword(User user, String password);

    TenantModel createPassword(String token, String password);

    void forgotPassword(String email);

    void deleteAccount(Tenant tenant);

    Boolean deleteCoTenant(Tenant tenant, Long coTenantId);

    void linkTenantToPartner(Tenant tenant, String partner, String internalPartnerId);

    void logout(String keycloakId);

    void unlinkFranceConnect(Tenant tenant);

}
