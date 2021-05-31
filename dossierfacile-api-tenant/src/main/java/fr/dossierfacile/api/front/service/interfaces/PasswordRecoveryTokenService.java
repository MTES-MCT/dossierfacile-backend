package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;

public interface PasswordRecoveryTokenService {
    PasswordRecoveryToken create(User user);
}
