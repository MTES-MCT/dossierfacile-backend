package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;

import java.io.ByteArrayOutputStream;

public interface ApartmentSharingPdfService {
    ByteArrayOutputStream generatePdf(ApartmentSharing apartmentSharing);
}
