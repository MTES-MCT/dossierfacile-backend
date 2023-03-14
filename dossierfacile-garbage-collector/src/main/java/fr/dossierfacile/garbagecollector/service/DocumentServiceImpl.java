package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.garbagecollector.model.document.GarbageDocument;
import fr.dossierfacile.garbagecollector.repo.document.DocumentRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ApartmentSharingRepository apartmentSharingRepository;

    @Override
    public void deleteAllDocumentsAssociatedToTenant(Tenant tenant) {
        List<GarbageDocument> documentList = documentRepository.findAllAssociatedToTenantId(tenant.getId());

        if (StringUtils.isNotBlank(tenant.getApartmentSharing().getUrlDossierPdfDocument())) {
            try {
                fileStorageService.delete(tenant.getApartmentSharing().getUrlDossierPdfDocument());
                tenant.getApartmentSharing().setUrlDossierPdfDocument("");
                apartmentSharingRepository.save(tenant.getApartmentSharing());
            } catch (Exception e) {
                log.error("Couldn't delete object [" + tenant.getApartmentSharing().getUrlDossierPdfDocument() + "] from apartment_sharing [" + tenant.getApartmentSharing().getId() + "]");
            }
        }

        documentRepository.deleteAll(Optional.ofNullable(documentList).orElse(new ArrayList<>()));
    }
}
