package fr.dossierfacile.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@UtilityClass
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

        try (PDDocument document = Loader.loadPDF(multipartFile.getInputStream().readAllBytes())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.error("Problem reading number of pages of document" + e.getMessage(), e);
        }
        return 0;
    }

    public static BufferedImage[] convertPdfToImage(File pdfFile) throws IOException {
        BufferedImage[] images = null;
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            images = new BufferedImage[document.getNumberOfPages()];
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); pageNumber++) {
                images[pageNumber] = pdfRenderer.renderImageWithDPI(pageNumber, 512);
            }
            return images;
        }
    }
}