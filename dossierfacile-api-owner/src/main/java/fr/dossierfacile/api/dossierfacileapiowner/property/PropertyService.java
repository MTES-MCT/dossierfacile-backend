package fr.dossierfacile.api.dossierfacileapiowner.property;

import java.util.List;

public interface PropertyService {

    PropertyModel createOrUpdate(PropertyForm propertyForm);

    List<PropertyModel> getAllProperties();

    void delete(Long id);
}
