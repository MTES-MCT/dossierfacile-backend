package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LinkLogRepository extends JpaRepository<LinkLog, Long> {

    List<LinkLog> findByApartmentSharingAndToken(ApartmentSharing apartmentSharing, UUID token);

}
