package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Lists the variants of the dossier PDF. Each one has its own template. */
@Getter
@RequiredArgsConstructor
public enum DossierPdfStatus {

    VALIDATED("validated", "Dossier de location validé par une personne de l'équipe DossierFacile"),
    /** This variant also covers the dossiers that wait for a verification. TODO: give them their own variant. */
    COMPLETED("completed", "Dossier de location complet, non vérifié par l'équipe DossierFacile");

    private final String template;
    /** The PDF metadata use this description. */
    private final String description;

    static DossierPdfStatus of(ApartmentSharing apartmentSharing) {
        return apartmentSharing.getStatus() == TenantFileStatus.VALIDATED ? VALIDATED : COMPLETED;
    }
}
