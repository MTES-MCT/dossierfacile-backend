package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ApartmentSharingService {

    ApplicationModel full(String token);

    ApplicationModel light(String token);

    ByteArrayOutputStream fullPdf(String token) throws IOException;

    void resetDossierPdfGenerated(ApartmentSharing apartmentSharing);

    void createFullPdf(String token);
}
