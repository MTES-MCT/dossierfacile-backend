package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

public interface PropertyService {

    PropertyModel createOrUpdate(PropertyForm propertyForm);

    List<PropertyModel> getAllProperties();

    void delete(Long id);

    Optional<Property> getProperty(Long id);

    Optional<Property> getPropertyByToken(String token);

    HttpResponse<String> subscribeTenantToProperty(String token, Long tenantId) throws IOException, InterruptedException;
}
