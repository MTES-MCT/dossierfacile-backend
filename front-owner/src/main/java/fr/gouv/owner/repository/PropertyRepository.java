package fr.gouv.owner.repository;


import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Property findOneByToken(String token);


    Property findOneByPropertyIdAndOwner(String propertyId, Owner owner);

    List<Property> findAllByOwner(Owner owner);

    Property findOneById(Long id);
}
