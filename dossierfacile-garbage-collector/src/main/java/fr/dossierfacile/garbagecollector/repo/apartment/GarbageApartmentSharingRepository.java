package fr.dossierfacile.garbagecollector.repo.apartment;

import fr.dossierfacile.garbagecollector.model.apartment.GarbageApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GarbageApartmentSharingRepository extends JpaRepository<GarbageApartmentSharing, Long> {

    @Query(value = "SELECT distinct apartment_sharing.* FROM apartment_sharing " +
            "left join tenant on apartment_sharing.id=tenant.apartment_sharing_id " +
            " where tenant.status='ARCHIVED' and apartment_sharing.url_dossier_pdf_document is not null and apartment_sharing.url_dossier_pdf_document <> '' " +
            " ORDER BY apartment_sharing.id LIMIT :limit", nativeQuery = true)
    List<GarbageApartmentSharing> getArchivedAptWithPdf(@Param("limit") Integer limit);

}
