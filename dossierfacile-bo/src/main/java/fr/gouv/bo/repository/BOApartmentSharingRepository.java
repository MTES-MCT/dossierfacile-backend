package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BOApartmentSharingRepository extends JpaRepository<ApartmentSharing, Long> {
    
    ApartmentSharing findOneByToken(String token);

    ApartmentSharing findOneByTokenPublic(String token);

}
