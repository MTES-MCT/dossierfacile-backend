package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.common.entity.ApartmentSharing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Lists the variants of the dossier PDF. The two complete dossiers share a template. */
@Getter
@RequiredArgsConstructor
public enum DossierPdfStatus {

    VALIDATED("validated", "Dossier de location validé par une personne de l'équipe DossierFacile"),
    COMPLETED("completed", "Dossier de location complet, non vérifié par l'équipe DossierFacile"),
    TO_PROCESS("completed", "Dossier de location complet, vérification en cours par l'équipe DossierFacile");

    private final String template;
    /** The PDF metadata use this description. */
    private final String description;

    static DossierPdfStatus of(ApartmentSharing apartmentSharing) {
        return switch (apartmentSharing.getStatus()) {
            case VALIDATED -> VALIDATED;
            case COMPLETED -> COMPLETED;
            case TO_PROCESS -> TO_PROCESS;
            default -> throw new IllegalStateException("No dossier PDF for the status " + apartmentSharing.getStatus());
        };
    }
}
