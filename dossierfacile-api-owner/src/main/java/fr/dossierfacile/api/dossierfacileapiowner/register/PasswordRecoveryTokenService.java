package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;

public interface PasswordRecoveryTokenService {
    PasswordRecoveryToken create(Owner owner);
}
