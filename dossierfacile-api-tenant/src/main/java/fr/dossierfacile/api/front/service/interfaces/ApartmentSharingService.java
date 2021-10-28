package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ApartmentSharingService {
    void createApartmentSharing(Tenant tenant);

    ApplicationModel full(String token);

    ApplicationModel light(String token);

    ByteArrayOutputStream fullPdf(String token) throws IOException;

    void resetDossierPdfGenerated(ApartmentSharing apartmentSharing);
}
