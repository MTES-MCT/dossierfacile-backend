package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinkLogRepository extends JpaRepository<LinkLog, Long> {

    List<LinkLog> findByApartmentSharingAndToken(ApartmentSharing apartmentSharing, String token);

}
