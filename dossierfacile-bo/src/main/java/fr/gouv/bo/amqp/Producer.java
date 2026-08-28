package fr.gouv.bo.amqp;

import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final QueueMessageService queueMessageService;

    public void generatePdf(Long documentId) {
        log.debug("Sending document with ID [{}] for pdf generation", documentId);
        queueMessageService.sendDocumentPendingMessage(documentId);
    }

}
