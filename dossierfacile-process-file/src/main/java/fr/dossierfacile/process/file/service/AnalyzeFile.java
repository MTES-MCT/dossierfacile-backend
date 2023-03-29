package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.FileAnalysisCriteria;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.service.qrcodeanalysis.QrCodeFileProcessor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnalyzeFile {

    private final ProcessTaxDocument processTaxDocument;
    private final DocumentService documentService;
    private final QrCodeFileProcessor qrCodeFileProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        fileRepository.findById(fileId)
                .filter(FileAnalysisCriteria::shouldBeAnalyzed)
                .ifPresent(file -> {
                    qrCodeFileProcessor.process(file);
                    processTaxDocument(file.getDocument());
                });
    }

    private void processTaxDocument(Document document) {
        if (document.getDocumentCategory() == DocumentCategory.TAX) {
            processTaxDocument.process(document)
                    .ifPresent(result -> documentService.updateTaxProcessResult(result, document));
        }
    }

}
