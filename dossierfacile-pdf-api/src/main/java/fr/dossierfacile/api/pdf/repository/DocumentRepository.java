package fr.dossierfacile.api.pdf.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findFirstByIdAndDocumentCategory(Long documentId, DocumentCategory documentCategory);
}
