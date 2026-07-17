package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class CheckDocumentForReprocessingDomainService {

    private final JpaDocumentRepository jpaDocumentRepository;
    private final MessagePublisher messagePublisher;

    public void checkDocumentsForReprocessing(Document document) {
        var listOfDocumentToCheck = new ArrayList<Document>();

        if (document.getTenantId() != null) {
            listOfDocumentToCheck.addAll(jpaDocumentRepository.getDocumentsByTenantId(document.getTenantId()));
        } else if (document.getGuarantorId() != null) {
            listOfDocumentToCheck.addAll(jpaDocumentRepository.getDocumentsByGuarantorId(document.getGuarantorId()));
        }

        listOfDocumentToCheck.forEach(it -> {
            it.resetValidateOrInProgressDocumentAfterFileDeleted();
            if (Boolean.TRUE.equals(it.getNoDocument())) {
                messagePublisher.sendDocumentForPdfGeneration(it.getId());
            }
            jpaDocumentRepository.save(it);
        });
    }
}
