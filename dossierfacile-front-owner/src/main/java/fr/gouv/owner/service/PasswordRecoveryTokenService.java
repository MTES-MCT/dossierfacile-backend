package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.gouv.owner.repository.PasswordRecoveryTokenRepository;
import fr.gouv.owner.utils.UtilsLocatio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PasswordRecoveryTokenService {

    @Autowired
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    public PasswordRecoveryToken savePasswordRecoveryToken(User user) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findOneByUser(user);
        if (passwordRecoveryToken == null) {
            passwordRecoveryToken = new PasswordRecoveryToken();
            passwordRecoveryToken.setUser(user);
            passwordRecoveryToken.setExpirationDate(LocalDateTime.now().plusDays(1));
            passwordRecoveryToken.setToken(UtilsLocatio.generateRandomString(8));
        } else {
            passwordRecoveryToken.setToken(UtilsLocatio.generateRandomString(8));
            passwordRecoveryToken.setExpirationDate(LocalDateTime.now().plusDays(7));
        }
        passwordRecoveryTokenRepository.save(passwordRecoveryToken);

        return passwordRecoveryToken;
    }
}
