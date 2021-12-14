package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;

public interface UserService {
    long confirmAccount(String token);

    TenantModel createPassword(String token, String password);

    void forgotPassword(String email);

    void deleteAccount(Tenant tenant);

    Boolean deleteCoTenant(Tenant tenant, Long id);

    void linkTenantToPartner(Tenant tenant, PartnerForm partnerForm);

    void linkTenantToPartner(Tenant tenant, String partner);

    void logout(Tenant tenant);
}
