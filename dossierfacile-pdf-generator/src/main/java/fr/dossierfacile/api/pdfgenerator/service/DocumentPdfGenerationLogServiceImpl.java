package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.DocumentPdfGenerationLogService;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPdfGenerationLogServiceImpl implements DocumentPdfGenerationLogService {

    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;

    @Transactional
    public void deactivateNewerMessages(Long documentId, Long logId) {
        //Disable new and duplicate messages currently in the queue
        documentPdfGenerationLogRepository.deactivateNewerMessages(documentId, logId);
    }

    @Transactional
    public void updateDocumentPdfGenerationLog(Long logId) {
        documentPdfGenerationLogRepository.updateDocumentPdfGenerationLog(logId);
    }

}
