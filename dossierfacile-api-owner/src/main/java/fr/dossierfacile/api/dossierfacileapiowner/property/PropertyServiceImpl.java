package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final AuthenticationFacade authenticationFacade;
    private final PropertyRepository propertyRepository;
    private final OwnerPropertyMapper propertyMapper;

    @Override
    public PropertyModel createOrUpdate(PropertyForm propertyForm) {
        Owner owner = authenticationFacade.getOwner();
        Property property;
        if (propertyForm.getId() != null) {
             property = propertyRepository.findByIdAndOwnerId(propertyForm.getId(), owner.getId()).orElse(new Property());
        } else {
            property = new Property();
            property.setName("Propriété");
        }
        if (propertyForm.getName() != null && !propertyForm.getName().isBlank()) {
            property.setName(propertyForm.getName());
        }
        if (propertyForm.getType() != null) {
            property.setType(propertyForm.getType());
        }
        if (propertyForm.getAddress() != null) {
            property.setAddress(propertyForm.getAddress());
        }
        if (propertyForm.getFurniture() != null) {
            property.setFurniture(propertyForm.getFurniture());
        }
        if (propertyForm.getRentCost() != null && propertyForm.getRentCost() > 0) {
            property.setRentCost(propertyForm.getRentCost());
        }
        if (propertyForm.getValidated() != null && propertyForm.getValidated()) {
            property.setValidated(true);
        }
        property.setOwner(owner);
        return propertyMapper.toPropertyModel(propertyRepository.save(property));
    }

    @Override
    public List<PropertyModel> getAllProperties() {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        return properties.stream().map(propertyMapper::toPropertyModel).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        Optional<Property> property = properties.stream().filter(p -> p.getId().equals(id)).findFirst();
        property.ifPresent(propertyRepository::delete);
    }
}
