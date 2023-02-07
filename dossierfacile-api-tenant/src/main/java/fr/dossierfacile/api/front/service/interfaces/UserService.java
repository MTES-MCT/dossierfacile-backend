package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.register.form.tenant.FranceConnectTaxForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    long confirmAccount(String token);

    TenantModel createPassword(User user, String password);

    TenantModel createPassword(String token, String password);

    void forgotPassword(String email);

    void deleteAccount(Tenant tenant);

    Boolean deleteCoTenant(Tenant tenant, Long coTenantId);

    void linkTenantToPartner(Tenant tenant, String partner, String internalPartnerId);

    void logout(Tenant tenant);

    void unlinkFranceConnect(Tenant tenant);

    @Transactional
    void checkDGFIPApi(Tenant tenant, FranceConnectTaxForm franceConnectTaxForm);
}
