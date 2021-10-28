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
    private static final float DPI_RENDERING_PDF_PAGE_TO_IMAGE = 150;

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

    public static int countNumberOfPagesOfPdfDocument(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return 0;
        }
        ByteArrayOutputStream outputStream = mergeMultipartFiles(Collections.singletonList(multipartFile));
        return countNumberOfPagesOfPdfDocument(outputStream.toByteArray());
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

    public static List<ByteArrayOutputStream> generateListOfImagesFromPdfPages(InputStream inputStream) {

        List<ByteArrayOutputStream> outputStreamList = new ArrayList<>();
        PDDocument document;
        try {
            document = PDDocument.load(inputStream);
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int indexPage = 0; indexPage < document.getNumberOfPages(); ++indexPage) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                BufferedImage bim = pdfRenderer.renderImageWithDPI(indexPage, DPI_RENDERING_PDF_PAGE_TO_IMAGE);
                ImageIOUtil.writeImage(bim, "png", outputStream);

                outputStreamList.add(
                        outputStream
                );
            }
            document.close();
        } catch (IOException e) {
            log.error("Problem converting the pdf pages to images");
            log.error(e.getMessage(), e.getCause());
        }
        return outputStreamList;
    }

    public static BufferedImage convertPdfTemplateToImage(InputStream inputStream) {
        BufferedImage bim = null;
        try {
            PDDocument document = PDDocument.load(inputStream);
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            bim = pdfRenderer.renderImageWithDPI(0, DPI_RENDERING_PDF_PAGE_TO_IMAGE);
            document.close();

        } catch (IOException e) {
            log.error("Problem in add water marker method");
            log.error("Problem converting the pdf page to image");
            log.error(e.getMessage(), e.getCause());
        }
        return bim;
    }

    public static void addParagraph(PDPageContentStream contentStream, float width, float sx, float sy,
                                    List<String> text, boolean justify, PDFont font, float fontSize) throws IOException {
        List<List<String>> listList = parseLines(text, width, font, fontSize);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(sx, sy);
        for (List<String> lines : listList
        ) {
            for (String line : lines) {
                float charSpacing = 0;
                if (justify && line.length() > 1) {
                    float size = fontSize * font.getStringWidth(line) / 1000;
                    float free = width - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }

                }
                contentStream.setCharacterSpacing(charSpacing);
                contentStream.showText(line);
                contentStream.newLine();
            }
        }
    }

    private static List<List<String>> parseLines(List<String> list, float width, PDFont font, float fontSize) throws IOException {
        List<List<String>> listArrayList = new ArrayList<>();
        for (String text : list
        ) {
            List<String> lines = new ArrayList<>();
            int lastSpace = -1;
            while (text.length() > 0) {
                int spaceIndex = text.indexOf(' ', lastSpace + 1);
                if (spaceIndex < 0)
                    spaceIndex = text.length();
                String subString = text.substring(0, spaceIndex);
                float size = fontSize * font.getStringWidth(subString) / 1000;
                if (size > width) {
                    if (lastSpace < 0) {
                        lastSpace = spaceIndex;
                    }
                    subString = text.substring(0, lastSpace);
                    lines.add(subString);
                    text = text.substring(lastSpace).trim();
                    lastSpace = -1;
                } else if (spaceIndex == text.length()) {
                    lines.add(text);
                    text = "";
                } else {
                    lastSpace = spaceIndex;
                }
            }
            listArrayList.add(lines);
        }
        return listArrayList;
    }
}
