package fr.dossierfacile.api.front.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class Utility {
    public static final PDRectangle MINUMUM_BASE_PDF_PAGE = PDRectangle.A4;


    public static int countNumberOfPagesOfPdfDocument(byte[] bytes) {
        int numberOfPages = 0;
        PDDocument document;
        try {
            document = PDDocument.load(bytes);
            numberOfPages = document.getNumberOfPages();
            document.close();
        } catch (IOException e) {
            log.error("Problem reading number of pages of document");
            log.error(e.getMessage(), e.getCause());
        }
        return numberOfPages;
    }

    public static ByteArrayOutputStream mergeMultipartFiles(List<MultipartFile> multipartFiles) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PDFMergerUtility ut = new PDFMergerUtility();
        try {
            for (MultipartFile f : multipartFiles) {
                if (Objects.equals(f.getContentType(), "application/pdf")) {
                    ut.addSource(f.getInputStream());
                } else {
                    ut.addSource(new ByteArrayInputStream(convertImgToPDF(f)));
                }
            }
            ut.setDestinationStream(outputStream);
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error("Exception while trying to merge documents", e);
        }
        return outputStream;
    }

    private static byte[] convertImgToPDF(MultipartFile multipartFile) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PDDocument document = new PDDocument()) {
            BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
            if (bufferedImage != null) {
                float width = bufferedImage.getWidth();
                float height = bufferedImage.getHeight();

                float ratioSourceImage = height / width;
                float ratioBasePDF = MINUMUM_BASE_PDF_PAGE.getHeight() / MINUMUM_BASE_PDF_PAGE.getWidth();

                PDPage page = new PDPage(MINUMUM_BASE_PDF_PAGE);
                document.addPage(page);
                PDImageXObject img = PDImageXObject.createFromByteArray(document, multipartFile.getBytes(), multipartFile.getOriginalFilename());
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                if (ratioSourceImage <= 1 || ratioSourceImage < ratioBasePDF) {
                    float newWidth = MINUMUM_BASE_PDF_PAGE.getWidth();
                    float newHeight = ratioSourceImage * newWidth;
                    float vDispl = (MINUMUM_BASE_PDF_PAGE.getHeight() - newHeight) / 2;
                    contentStream.drawImage(img, 0, vDispl, newWidth, newHeight);
                } else if (ratioSourceImage > ratioBasePDF) {
                    float newHeight = MINUMUM_BASE_PDF_PAGE.getHeight();
                    float newWidth = newHeight / ratioSourceImage;
                    float hDispl = (MINUMUM_BASE_PDF_PAGE.getWidth() - newWidth) / 2;
                    contentStream.drawImage(img, hDispl, 0, newWidth, newHeight);
                } else {
                    contentStream.drawImage(img, 0, 0, MINUMUM_BASE_PDF_PAGE.getWidth(), MINUMUM_BASE_PDF_PAGE.getHeight());
                }

                contentStream.close();
                document.save(baos);
            }
        } catch (IOException e) {
            log.error("Exception while trying convert image to pdf", e);
        }
        return baos.toByteArray();
    }
}
