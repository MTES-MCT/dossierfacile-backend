package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    @Override
    public boolean documentIsUpToDateAt(Long timestamp, Long documentId) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        Document document = documentRepository.findById(documentId).orElse(null);

        return document != null && dateTime.isAfter(document.getLastModifiedDate());
    }

    @Override
    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

}
