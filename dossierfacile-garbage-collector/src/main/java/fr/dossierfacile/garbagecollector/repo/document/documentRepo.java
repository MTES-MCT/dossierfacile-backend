package fr.dossierfacile.garbagecollector.repo.document;

import fr.dossierfacile.garbagecollector.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface documentRepo extends JpaRepository<Document, Long> {
}
