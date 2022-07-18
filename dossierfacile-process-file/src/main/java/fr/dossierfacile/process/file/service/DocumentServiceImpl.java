package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public void updateTaxProcessResult(TaxDocument taxProcessResult, Long documentId) {
        documentRepository.updateTaxProcessResult(taxProcessResult, documentId);
    }
}
