package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;

public interface ConfirmationTokenService {
    ConfirmationToken createToken(User user);

    ConfirmationToken findByToken(String token);
}
