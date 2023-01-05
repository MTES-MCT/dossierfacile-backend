package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentHelperService {
    /**
     * Build, upload and add the file inside the document.
     * Document list is updated.
     */
    File addFile(MultipartFile multipartFile, Document document);

    /**
     * Upload byte array to s3.
     */
    String addByteArrayToS3(byte[] file, String extension);

    /**
     * Delete files contained in document.
     */
    void deleteFiles(Document document);
}
