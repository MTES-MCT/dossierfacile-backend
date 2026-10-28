package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByUser(User user);

    @Query("SELECT token FROM PasswordRecoveryToken token WHERE token.token = :token AND TYPE(token.user) = Tenant")
    Optional<PasswordRecoveryToken> findTenantTokenByToken(@Param("token") String token);
}
