package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {
    Optional<ConfirmationToken> findByUser(User user);

    Optional<ConfirmationToken> findByToken(String token);
}
