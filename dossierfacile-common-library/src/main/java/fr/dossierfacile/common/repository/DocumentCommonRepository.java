package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCommonRepository extends JpaRepository<Document, Long> {
}