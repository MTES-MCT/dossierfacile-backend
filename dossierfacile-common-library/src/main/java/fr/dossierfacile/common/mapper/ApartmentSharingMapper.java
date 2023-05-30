package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

public interface ApartmentSharingMapper {
    ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    default ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing, UserApi userApi) {
        throw new UnsupportedOperationException();
    }
}
