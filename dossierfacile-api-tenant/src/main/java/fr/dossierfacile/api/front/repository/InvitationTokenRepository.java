package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {
    Optional<InvitationToken> findByApartmentSharingId(Long apartmentSharingId);

    Optional<InvitationToken> findByToken(String token);

    List<InvitationToken> findAllByCreatedDateBefore(LocalDateTime date);
}
