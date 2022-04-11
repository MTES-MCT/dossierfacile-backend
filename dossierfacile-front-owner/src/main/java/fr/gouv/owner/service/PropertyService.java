package fr.gouv.owner.service;


import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.gouv.owner.dto.PropertyDTO;
import fr.gouv.owner.repository.PropertyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    public void addVisit(Property property) {
        Integer countVisit = property.getCountVisit();
        if (countVisit == null) {
            property.setCountVisit(1);
        } else {
            property.setCountVisit(property.getCountVisit() + 1);
        }
        propertyRepository.save(property);
    }

    /**
     * @param propertyDTO
     * @param owner       used for AOP
     */

    public void create(PropertyDTO propertyDTO, Owner owner) {
        Property property = new Property(owner, propertyDTO.getName(), propertyDTO.getPropertyId(), propertyDTO.getRentCost());
        propertyRepository.save(property);
    }


    public List<Property> getPropertyList(Owner owner){
        return propertyRepository.findAllByOwner(owner);
    }

    public Property findOne(Long id) {
        return propertyRepository.getOne(id);
    }

    public List<Property> findAllByOwner(Owner owner) {
        return propertyRepository.findAllByOwner(owner);
    }

    public Property findOneByToken(String token) {
        return propertyRepository.findOneByToken(token);
    }

    public void delete(Long property) {
        propertyRepository.deleteById(property);
    }

    public void save(Property property) {
        propertyRepository.save(property);
    }
}
