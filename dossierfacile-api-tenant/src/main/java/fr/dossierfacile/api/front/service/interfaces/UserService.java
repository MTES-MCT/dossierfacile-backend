package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.DeleteAccountForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;

public interface UserService {
    void confirmAccount(String token);

    TenantModel createPassword(String token, String password);

    void forgotPassword(String email);

    Boolean deleteAccount(Tenant tenant, DeleteAccountForm deleteAccountForm);
}
