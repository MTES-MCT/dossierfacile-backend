package fr.dossierfacile.api.pdf.repository;

import fr.dossierfacile.common.entity.DocumentToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTokenRepository extends JpaRepository<DocumentToken, Long> {
    Optional<DocumentToken> findFirstByDocumentId(Long documentId);

    Optional<DocumentToken> findFirstByToken(String token);
}
