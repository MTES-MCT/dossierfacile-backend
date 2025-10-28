package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApartmentSharingLinkRepository extends JpaRepository<ApartmentSharingLink, Long> {

    List<ApartmentSharingLink> findByApartmentSharingAndCreationDateIsAfterAndDeletedIsFalse(ApartmentSharing apartmentSharing, LocalDateTime creationDate);

    List<ApartmentSharingLink> findByApartmentSharingAndLinkTypeAndDeletedIsFalse(ApartmentSharing apartmentSharing, ApartmentSharingLinkType linkType);

    List<ApartmentSharingLink> findByApartmentSharingOrderByCreationDate(ApartmentSharing apartmentSharing);

    Optional<ApartmentSharingLink> findByIdAndApartmentSharingAndDeletedIsFalse(Long id, ApartmentSharing apartmentSharing);

    Optional<ApartmentSharingLink> findByToken(UUID token);

    @Query(value = """
            SELECT *
            FROM apartment_sharing_link
            WHERE token = :token
            AND deleted = false
            AND disabled = false
            AND full_data = :fullData
            AND (expiration_date IS NULL OR expiration_date > NOW())
            """, nativeQuery = true)
    Optional<ApartmentSharingLink> findValidLinkByToken(@Param("token") UUID token, @Param("fullData") boolean fullData);

}
