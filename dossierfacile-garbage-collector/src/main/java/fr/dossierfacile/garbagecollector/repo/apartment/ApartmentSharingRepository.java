package fr.dossierfacile.garbagecollector.repo.apartment;

import fr.dossierfacile.garbagecollector.model.apartment.ApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentSharingRepository extends JpaRepository<ApartmentSharing, Long> {

    @Query(value = "SELECT distinct apartment_sharing.* FROM apartment_sharing " +
            "left join tenant on apartment_sharing.id=tenant.apartment_sharing_id " +
            " where tenant.status='ARCHIVED' and apartment_sharing.url_dossier_pdf_document is not null and apartment_sharing.url_dossier_pdf_document <> '' " +
            " ORDER BY apartment_sharing.id LIMIT :limit", nativeQuery = true)
    List<ApartmentSharing> getArchivedAptWithPdf(@Param("limit") Integer limit);

    @Query(value = "SELECT distinct document.* FROM document " +
            "left join tenant on document.tenant_id=tenant.id " +
            " where tenant.status='ARCHIVED' and document.name is not null and document.name <> '' " +
            " ORDER BY document.id LIMIT :limit", nativeQuery = true)
    List<ApartmentSharing> getArchivedAptWithDocumentWithPdf(@Param("limit") Integer limit);
}
