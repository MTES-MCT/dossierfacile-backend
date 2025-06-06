package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface PropertyService {

    PropertyModel createOrUpdate(PropertyForm propertyForm) throws HttpResponseException, InterruptedException, IOException;

    List<PropertyModel> getAllProperties();

    void delete(Long id);

    Optional<Property> getProperty(Long id);

    Optional<Property> getPropertyByToken(String token);

    void subscribeTenantToProperty(String token, String kcTenantToken);

    void logAccess(Property property);

    void deleteDpe(Property property);
}
