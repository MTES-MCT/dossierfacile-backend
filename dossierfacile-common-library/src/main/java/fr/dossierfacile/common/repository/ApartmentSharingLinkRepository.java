package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApartmentSharingLinkRepository extends JpaRepository<ApartmentSharingLink, Long> {
    Optional<ApartmentSharingLink> findByTokenAndFullDataAndDisabledIsFalse(String token, boolean fullData);

    List<ApartmentSharingLink> findByApartmentSharingAndCreationDateIsAfter(ApartmentSharing apartmentSharing, LocalDateTime creationDate);

    List<ApartmentSharingLink> findByApartmentSharingAndLinkType(ApartmentSharing apartmentSharing, ApartmentSharingLinkType linkType);

    Optional<ApartmentSharingLink> findByIdAndApartmentSharing(Long id, ApartmentSharing apartmentSharing);

}
