package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentHelperService {
    /**
     * Build, upload and add the file inside the document.
     * Document list is updated.
     */
    File addFile(MultipartFile multipartFile, Document document) throws IOException;

    InputStream convertHeicToJpg(InputStream heicInputStream) throws IOException;

    /**
     * Delete files contained in document.
     */
    void deleteFiles(Document document);

    StorageFile generatePreview(Document document, InputStream fileInputStream, String originalName);
}
