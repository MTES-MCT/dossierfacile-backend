package fr.dossierfacile.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;

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

    /**
     * Counts PDF pages or returns 1 for non-PDF. Uses detectedMimeType (Tika) instead of
     * client-supplied Content-Type.
     */
    public static int countNumberOfPagesOfPdfDocument(MultipartFile multipartFile, String detectedMimeType) {
        if (multipartFile.isEmpty()) {
            return 0;
        }
        if (!"application/pdf".equals(detectedMimeType)) {
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

    /**
     * OWASP File Upload — "Set a filename length limit. Restrict the allowed characters if possible".
     * <p>
     * Sanitizes the given filename for safe use in HTTP headers and file systems.
     * Returns "file" if input is null, blank, or results in an empty string after sanitization.
     */
    public static String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "file";
        }

        // 1. Normalisation : Sépare les lettres de leurs accents (ex: "é" devient "e" + "´")
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. Suppression des accents via une expression régulière
        // \p{M} cible toutes les marques diacritiques (les accents flottants)
        String withoutAccents = normalized.replaceAll("\\p{M}", "");

        // 3. Remplacement des espaces par des underscores (très recommandé pour les headers HTTP)
        String noSpaces = withoutAccents.replaceAll("\\s+", "_");

        // 4. Suppression de tout ce qui n'est pas alphanumérique, point, tiret ou underscore.
        // Cela supprime les guillemets, slashs, emojis et caractères spéciaux interdits par Windows/Linux.
        String result = noSpaces.replaceAll("[^a-zA-Z0-9._-]", "");

        // OWASP: Si le résultat est vide (ex: input "@@@"), retourner "file"
        return result.isBlank() ? "file" : result;
    }

    /**
     * OWASP File Upload — "Set a filename length limit".
     * Sanitizes the filename and truncates it if longer than maxLength, preserving the extension.
     */
    public static String sanitizeAndTruncateFilename(String input) {
        return sanitizeAndTruncateFilename(input, 200);
    }

    /**
     * OWASP File Upload — "Set a filename length limit".
     * Sanitizes the filename and truncates it if longer than maxLength, preserving the extension.
     */
    public static String sanitizeAndTruncateFilename(String input, int maxLength) {
        String displayName = sanitizeFilename(input);
        if (displayName.length() <= maxLength) {
            return displayName;
        }
        String ext = FilenameUtils.getExtension(displayName);
        String base = FilenameUtils.getBaseName(displayName);
        int maxBase = maxLength - (ext.isEmpty() ? 0 : ext.length() + 1);
        return base.substring(0, Math.min(base.length(), maxBase))
                + (ext.isEmpty() ? "" : "." + ext);
    }

    /**
     * Streams file content to HTTP response with standard headers.
     *
     * @param inputStream the file content stream (caller is responsible for closing)
     * @param contentType the MIME type
     * @param filename    optional filename for Content-Disposition (null uses "file")
     * @param inline      true for inline display, false for attachment (download)
     * @param response    the HTTP response
     * @throws IOException if streaming fails
     */
    public static void streamFileToResponse(InputStream inputStream, String contentType,
                                            @Nullable String filename, boolean inline,
                                            HttpServletResponse response) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type");
        ContentDisposition contentDisposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(sanitizeFilename(filename != null ? filename : "file"))
                .build();
        response.setHeader("Content-Disposition", contentDisposition.toString());
        response.setHeader("X-Robots-Tag", "noindex");
        IOUtils.copy(inputStream, response.getOutputStream());
    }
}