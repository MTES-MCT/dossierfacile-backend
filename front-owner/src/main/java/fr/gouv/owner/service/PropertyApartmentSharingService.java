package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.gouv.owner.repository.PropertyApartmentSharingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PropertyApartmentSharingService {

    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;

    public List<PropertyApartmentSharing> getAllPropertyApartmentSharing(List<Property> propertyList){
        return propertyApartmentSharingRepository.findAllByPropertyId(propertyList.get(0).getId());

    }

    public void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing){
        propertyApartmentSharingRepository.delete(propertyApartmentSharing);
    }
}
