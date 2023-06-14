package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.WatermarkDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WatermarkDocumentRepository extends JpaRepository<WatermarkDocument, Long> {
    Optional<WatermarkDocument> findOneByToken(String token);

    List<WatermarkDocument> findAllByCreatedDateBefore(LocalDateTime date);
}