package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;

public interface DocumentService {
    void delete(Long id);

    void generatePdfByFilesOfDocument(Document document);

    void updateOthersDocumentsStatus(Tenant tenant);
}
