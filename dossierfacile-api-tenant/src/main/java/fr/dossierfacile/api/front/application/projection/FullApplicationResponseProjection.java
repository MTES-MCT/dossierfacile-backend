package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Full view for a tokenized link: dossier links + document names rewritten to the link path,
 * only for documents that have a watermark file.
 */
@Component
public class FullApplicationResponseProjection extends SharedApplicationResponseProjection {

    private static final String DOCUMENT_LINK_PATH = "api/application/links";
    private static final String DOSSIER_PDF_PATH = "api/application/fullPdf";
    private static final String DOSSIER_PATH = "file";

    // Same defaults as the legacy mappers
    @Value("${application.base.url:default}")
    private String applicationBaseUrl;

    @Value("${tenant.base.url:default}")
    private String tenantBaseUrl;

    public ApplicationModel project(ApplicationProjectionSources sources, UUID token) {
        String documentUrlPrefix = applicationBaseUrl + "/" + DOCUMENT_LINK_PATH + "/" + token + "/documents/";
        ApplicationModel model = assemble(sources,
                document -> document.hasWatermarkFile() ? documentUrlPrefix + document.getName() : null);
        model.setDossierPdfUrl(applicationBaseUrl + "/" + DOSSIER_PDF_PATH + "/" + token);
        model.setDossierUrl(tenantBaseUrl + "/" + DOSSIER_PATH + "/" + token);
        return model;
    }
}
