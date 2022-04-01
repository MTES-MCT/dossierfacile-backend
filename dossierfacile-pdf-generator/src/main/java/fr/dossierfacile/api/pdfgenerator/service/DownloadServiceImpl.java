package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.exception.DocumentNotFoundException;
import fr.dossierfacile.api.pdfgenerator.exception.FileDownloadException;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.service.interfaces.OvhService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@AllArgsConstructor
@Slf4j
public class DownloadServiceImpl implements DownloadService {
    private final OvhService ovhService;

    private InputStream getDownloadedInputStream(String file) throws FileDownloadException {
        SwiftObject swiftObject = ovhService.get(file);
        if (swiftObject != null) {
            DLPayload dlPayload = swiftObject.download();
            if (dlPayload.getHttpResponse().getStatus() == HttpStatus.OK.value()) {
                return dlPayload.getInputStream();
            }
        }
        throw new FileDownloadException(file);
    }

    @Override
    public InputStream getDocumentInputStream(Document document) {
        log.info("Downloading document [" + document.getDocumentCategory() + ", " + document.getName() + "]");
        if (StringUtils.isBlank(document.getName())) {
            throw new DocumentNotFoundException(document);
        }
        try {
            return getDownloadedInputStream(document.getName());
        } catch (Exception e) {
            throw new FileDownloadException(document);
        }
    }

    @Override
    public InputStream getFileInputStream(String filepath) {
        log.info("Downloading file [" + filepath + "]");
        if (StringUtils.isBlank(filepath)) {
            throw new IllegalArgumentException("File path is empty");
        }
        return getDownloadedInputStream(filepath);
    }
}
