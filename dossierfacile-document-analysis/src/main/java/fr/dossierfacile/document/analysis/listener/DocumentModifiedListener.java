package fr.dossierfacile.document.analysis.listener;

import fr.dossierfacile.common.domain.event.DocumentModifiedEvent;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentModifiedListener {

    private final DocumentCommonRepository documentRepository;
    private final DocumentIAService documentIAService;

    // S'exécute dans un thread séparé pour ne pas bloquer le traitement principal de l'utilisateur
    @Async
    // Ouvre une nouvelle transaction dédiée à ce thread asynchrone (indispensable pour les manipulations d'entités)
    @Transactional
    // S'exécute uniquement si la transaction principale a réussi (commit)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentModified(DocumentModifiedEvent event) {
        log.info("Handling DocumentModifiedEvent for documentId={}", event.documentId());
        var document = documentRepository.findById(event.documentId()).orElse(null);
        if (document != null) {
            documentIAService.analyseDocument(document);
        } else {
            log.warn("Document with id={} not found. Cannot trigger IA analysis.", event.documentId());
        }
    }
}
