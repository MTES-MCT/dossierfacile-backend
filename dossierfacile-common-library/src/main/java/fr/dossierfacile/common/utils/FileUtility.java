package fr.dossierfacile.common.utils;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class FileUtility {

    public static String computeMediaType(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        switch (extension) {
            case "pdf":
                return MediaType.APPLICATION_PDF_VALUE;
            case "jpg", "jpeg":
                return MediaType.IMAGE_JPEG_VALUE;
            case "png":
                return MediaType.IMAGE_PNG_VALUE;
        }
        // default contentType for files
        return MediaType.IMAGE_PNG_VALUE;
    }

    public static int countNumberOfPagesOfPdfDocument(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return 0;
        }
        if (!Objects.equals(multipartFile.getContentType(), "application/pdf")) {
            return 1;
        }

        try (PDDocument document = PDDocument.load(multipartFile.getInputStream())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.error("Problem reading number of pages of document");
            log.error(e.getMessage(), e.getCause());
            Sentry.captureException(e);
        }
        return 0;
    }
}