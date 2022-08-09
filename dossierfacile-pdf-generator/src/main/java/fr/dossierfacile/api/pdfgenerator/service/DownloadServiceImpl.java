package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.exception.DocumentNotFoundException;
import fr.dossierfacile.api.pdfgenerator.exception.FileDownloadException;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@AllArgsConstructor
@Slf4j
public class DownloadServiceImpl implements DownloadService {
    private final FileStorageService fileStorageService;

    @Override
    public InputStream getDocumentInputStream(Document document) {
        log.info("Downloading document [" + document.getDocumentCategory() + ", " + document.getName() + "]");
        if (StringUtils.isBlank(document.getName())) {
            throw new DocumentNotFoundException(document);
        }
        try {
            return fileStorageService.download(document.getName(), null);
        } catch (Exception e) {
            throw new FileDownloadException(document);
        }
    }
}
