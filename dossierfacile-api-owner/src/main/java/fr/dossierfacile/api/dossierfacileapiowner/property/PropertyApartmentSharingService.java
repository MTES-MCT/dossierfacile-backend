package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;

public interface PropertyApartmentSharingService {

    void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing);

    void subscribeTenantApartmentSharingToProperty(Tenant tenant, Property property, boolean hasAccess);
}
