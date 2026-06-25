package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository d'infrastructure pour l'agrégat Document.
 * Cette classe encapsule le repository Spring Data JPA pour DocumentEntity
 * et convertit/enveloppe les entités de persistance dans le type du domaine public {@link Document}.
 */
@Repository
public class JpaDocumentRepository {

    private final JpaDocumentEntityRepository jpaDocumentEntityRepository;

    JpaDocumentRepository(JpaDocumentEntityRepository jpaDocumentEntityRepository) {
        this.jpaDocumentEntityRepository = jpaDocumentEntityRepository;
    }

    /**
     * Recherche un document par son ID.
     */
    public Optional<Document> findById(Long id) {
        return jpaDocumentEntityRepository.findById(id)
                .map(Document::new);
    }

    /**
     * Recherche un document contenant un fichier spécifique par son ID.
     */
    public Optional<Document> findByFileId(Long fileId) {
        return jpaDocumentEntityRepository.findByFileId(fileId)
                .map(Document::new);
    }

    /**
     * Récupère tous les documents associés à un tenant.
     */
    public List<Document> getDocumentsByTenantId(Long tenantId) {
        return jpaDocumentEntityRepository.findAllByTenantId(tenantId).stream()
                .map(Document::new)
                .toList();
    }

    /**
     * Récupère tous les documents associés à un garant.
     */
    public List<Document> getDocumentsByGuarantorId(Long guarantorId) {
        return jpaDocumentEntityRepository.findAllByGuarantorId(guarantorId).stream()
                .map(Document::new)
                .toList();
    }

    /**
     * Sauvegarde l'état de l'agrégat en persistant l'entité interne.
     */
    public void save(Document document) {
        jpaDocumentEntityRepository.save(document.getEntity());
    }

    public void delete(Document document) {
        jpaDocumentEntityRepository.delete(document.getEntity());
    }

    /**
     * Sauvegarde et valide immédiatement en base de données.
     */
    public void saveAndFlush(Document document) {
        jpaDocumentEntityRepository.saveAndFlush(document.getEntity());
    }
}

/**
 * Interface Spring Data JPA interne (package-private) pour la persistence de DocumentEntity.
 * Non visible en dehors de ce package pour forcer l'usage exclusif de JpaDocumentRepository.
 */
interface JpaDocumentEntityRepository extends JpaRepository<DocumentEntity, Long> {

    @Query("select d from DocumentEntity d join d.files f where f.id = :fileId")
    Optional<DocumentEntity> findByFileId(@Param("fileId") Long fileId);

    List<DocumentEntity> findAllByTenantId(Long tenantId);

    List<DocumentEntity> findAllByGuarantorId(Long guarantorId);
}
