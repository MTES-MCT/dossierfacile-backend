package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyService {

    PropertyModel createOrUpdate(PropertyForm propertyForm);

    List<PropertyModel> getAllProperties();

    void delete(Long id);

    Optional<Property> getProperty(Long id);
}
