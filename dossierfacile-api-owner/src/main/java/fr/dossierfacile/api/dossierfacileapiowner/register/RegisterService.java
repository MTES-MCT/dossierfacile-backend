package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerModel;

public interface RegisterService {
    long confirmAccount(String token);

    OwnerModel register(AccountForm accountForm);

    void forgotPassword(String email);

    OwnerModel createPassword(String token, String password);
}
