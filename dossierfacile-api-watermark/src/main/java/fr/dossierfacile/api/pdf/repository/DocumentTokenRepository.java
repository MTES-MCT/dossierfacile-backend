package fr.dossierfacile.api.pdf.repository;

import fr.dossierfacile.common.entity.DocumentToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentTokenRepository extends JpaRepository<DocumentToken, Long> {
    Optional<DocumentToken> findFirstByDocumentId(Long documentId);

    Optional<DocumentToken> findFirstByToken(String token);

    List<DocumentToken> findAllByCreationDateBefore(LocalDateTime date);
}
