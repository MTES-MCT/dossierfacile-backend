package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyService {

    PropertyModel createOrUpdate(PropertyForm propertyForm);

    List<PropertyModel> getAllProperties();

    void delete(Long id);

    Optional<Property> getProperty(Long id);

    Optional<Property> getPropertyByToken(String token);

    void subscribeTenantToProperty(String token, String kcTenantToken);

    void logAccess(Property property);
}
