package fr.gouv.bo.service;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.gouv.bo.repository.GuarantorRepository;
import fr.gouv.bo.security.BOAccessDenied;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;

@Service
@RequiredArgsConstructor
@Slf4j
public class BOTenantResolver {

    private final SharedFileRepository sharedFileRepository;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;

    public Tenant resolveTenantFromFile(File file) {
        Document document = file.getDocument();
        if (document == null) {
            log.warn("BO access denied: document missing for file id={}", file.getId());
            throw BOAccessDenied.generic();
        }
        return resolveTenantFromDocument(document);
    }

    public Tenant resolveTenantFromFile(Long fileId) {
        File file = sharedFileRepository.findById(fileId).orElse(null);
        if (file == null) {
            log.warn("BO access denied: file not found id={}", fileId);
            throw BOAccessDenied.generic();
        }
        return resolveTenantFromFile(file);
    }

    public Tenant resolveTenantFromGuarantor(Guarantor guarantor) {
        Tenant tenant = guarantor.getTenant();
        if (tenant == null) {
            log.warn("BO access denied: tenant missing for guarantor id={}",
                    guarantor.getId());
            throw BOAccessDenied.generic();
        }
        return tenant;
    }

    public Tenant resolveTenantFromGuarantor(Long guarantorId) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId).orElse(null);
        if (guarantor == null) {
            log.warn("BO access denied: guarantor not found id={}", guarantorId);
            throw BOAccessDenied.generic();
        }
        return resolveTenantFromGuarantor(guarantor);
    }

    public Tenant resolveTenantFromDocument(Document document) {
        Tenant tenant = document.getGuarantor() == null ? document.getTenant() : document.getGuarantor().getTenant();
        if (tenant == null) {
            log.warn("BO access denied: tenant missing for document id={}", document.getId());
            throw BOAccessDenied.generic();
        }
        return tenant;
    }

    public Tenant resolveTenantFromDocument(Long documentId) {
        Document document;
        try {
            document = documentService.findDocumentById(documentId);
        } catch (DocumentNotFoundException e) {
            log.warn("BO access denied: document not found id={}", documentId);
            throw BOAccessDenied.generic();
        }
        return resolveTenantFromDocument(document);
    }
}
