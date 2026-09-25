package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentDeletionSource;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.DocumentDeletionCommonService;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class DocumentDeletionCommonServiceImpl implements DocumentDeletionCommonService {

    private final DocumentCommonRepository documentRepository;
    private final TenantCommonRepository tenantRepository;
    private final LogService logService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Tenant deleteDocument(Document document, Long operatorId, DocumentDeletionSource source) {
        Person owner = resolveOwner(document);
        Tenant tenant = resolveTenant(document);
        log.info("Deleting document [{}] of tenant [{}] (operator: {}, source: {})", document.getId(), tenant.getId(), operatorId, source);

        logService.saveDocumentDeletedLog(document, tenant, operatorId, source);

        // Keep the in-memory model consistent
        owner.getDocuments().removeIf(d -> Objects.equals(d.getId(), document.getId()));
        documentRepository.delete(document);

        // Any document deletion invalidates the auto-validation queue entry and the full dossier PDF
        tenant.setReadyForAutoValidation(false);
        tenantRepository.save(tenant);
        apartmentSharingCommonService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        return tenant;
    }

    private static Person resolveOwner(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant();
        }
        if (document.getGuarantor() != null) {
            return document.getGuarantor();
        }
        throw new IllegalStateException("Document " + document.getId() + " has neither tenant nor guarantor");
    }

    private static Tenant resolveTenant(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant();
        }
        Tenant tenant = document.getGuarantor().getTenant();
        if (tenant == null) {
            throw new IllegalStateException("Guarantor " + document.getGuarantor().getId() + " of document " + document.getId() + " has no tenant");
        }
        return tenant;
    }
}
