package fr.dossierfacile.api.front.application.projection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the full-variant read view for a tokenized link: dossier links and document
 * names rewritten to the link path, only for documents that have a watermark file.
 * Holds the variant knowledge only — the structural walk lives in ApplicationReadView#from.
 */
@Component
public class FullApplicationViewAssembler {

    private static final String DOCUMENT_LINK_PATH = "api/application/links";
    private static final String DOSSIER_PDF_PATH = "api/application/fullPdf";
    private static final String DOSSIER_PATH = "file";

    private final String applicationBaseUrl;
    private final String tenantBaseUrl;

    // Same defaults as the legacy mappers; constructor injection keeps the class instantiable without Spring
    public FullApplicationViewAssembler(
            @Value("${application.base.url:default}") String applicationBaseUrl,
            @Value("${tenant.base.url:default}") String tenantBaseUrl) {
        this.applicationBaseUrl = applicationBaseUrl;
        this.tenantBaseUrl = tenantBaseUrl;
    }

    public ApplicationReadView assemble(ApplicationProjectionSources sources, UUID token) {
        String documentUrlPrefix = applicationBaseUrl + "/" + DOCUMENT_LINK_PATH + "/" + token + "/documents/";
        return ApplicationReadView.from(
                sources,
                document -> document.hasWatermarkFile() ? documentUrlPrefix + document.getName() : null,
                applicationBaseUrl + "/" + DOSSIER_PDF_PATH + "/" + token,
                tenantBaseUrl + "/" + DOSSIER_PATH + "/" + token);
    }
}
