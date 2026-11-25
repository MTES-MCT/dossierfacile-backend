package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfRenderer;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@AllArgsConstructor
public class ApartmentSharingPdfDocumentTemplate implements PdfTemplate<ApartmentSharing> {

    private final TenantCommonRepository tenantRepository;
    private final DossierPdfRenderer dossierPdfRenderer;

    @Override
    public InputStream render(ApartmentSharing apartmentSharing) throws IOException {
        checkAllTenantsCompletedOrValidatedAndAllDocumentsNotNull(apartmentSharing.getId());
        return dossierPdfRenderer.render(apartmentSharing);
    }

    private void checkAllTenantsCompletedOrValidatedAndAllDocumentsNotNull(long apartmentSharingId) {
        int numberOfTenants = tenantRepository.countTenantsBlockingFullPdfGeneration(apartmentSharingId);
        if (numberOfTenants > 0) {
            throw new ApartmentSharingUnexpectedException(apartmentSharingId);
        }
    }
}
