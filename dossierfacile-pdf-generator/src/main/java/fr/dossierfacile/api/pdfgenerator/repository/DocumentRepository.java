package fr.dossierfacile.api.pdfgenerator.repository;

import fr.dossierfacile.common.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
