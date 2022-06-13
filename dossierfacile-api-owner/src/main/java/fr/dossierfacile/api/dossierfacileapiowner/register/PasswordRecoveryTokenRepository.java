package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByUser(User user);

    Optional<PasswordRecoveryToken> findByToken(String token);
}
