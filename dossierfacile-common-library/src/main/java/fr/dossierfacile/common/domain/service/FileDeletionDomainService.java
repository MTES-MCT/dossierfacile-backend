package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@AllArgsConstructor
public class FileDeletionDomainService {

    private final AddLogDomainService addLogDomainService;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final MessagePublisher messagePublisher;

    public Optional<Document> deleteFile(
            Long fileId,
            Document document,
            Tenant targetTenant,
            Optional<Operator> operator
    ) {
        FileEntity file = document.getFileById(fileId);

        addLogDomainService.addFileDeletedLog(file, targetTenant, operator);

        document.deleteFile(fileId);
        // Si le document n'est pas vide on re-analyse le document
        if (document.hasFiles()) {
            messagePublisher.sendDocumentForPdfGeneration(document.getId());
            jpaDocumentRepository.save(document);
            return Optional.of(document);
        }

        // Si le document est vide, on le supprime.
        // Je ne suis pas fan de ça, mais je l'ai implémenté quand même !
        addLogDomainService.addDocumentDeletedLog(document, targetTenant, operator);

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

        // fin du bloque que j'aime pas

        jpaDocumentRepository.delete(document);
        return Optional.empty();
    }
}
