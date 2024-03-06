package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.OperationAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationAccessTokenRepository extends JpaRepository<OperationAccessToken, Long> {
    List<OperationAccessToken> findAllByExpirationDateBefore(LocalDateTime now);

    void delete(OperationAccessToken token);

    OperationAccessToken findByToken(String token);
}
