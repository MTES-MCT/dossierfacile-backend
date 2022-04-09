package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.model.PageDimension;
import fr.dossierfacile.api.pdfgenerator.model.PdfTemplateParameters;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class BOPdfDocumentTemplate implements PdfTemplate<List<FileInputStream>> {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private final PdfTemplateParameters params = PdfTemplateParameters.builder().build();

    @Override
    public InputStream render(List<FileInputStream> data) throws IOException {

        try (PDDocument document = new PDDocument()) {

            data.stream()
                    .map(pdfFileIS -> convertToImages(pdfFileIS))
                    .flatMap(Collection::stream)
                    .map(bim -> fitImageToPage(bim))
                    .map(bim -> applyWatermark(bim))
                    .forEach(bim -> addImageAsPageToDocument(document, bim));

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.save(baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        } catch (IOException e) {
            log.error("Exception while generate BO PDF documents", e);
            log.error(EXCEPTION + Sentry.captureException(e));
            throw e;
        }

    }

    /**
     * Convert PDF to image - let other type unchanged
     *
     * @param fileInputStream source
     * @return list of extracted images
     */
    private List<BufferedImage> convertToImages(FileInputStream fileInputStream) {
        try {
            if ("pdf".equalsIgnoreCase(fileInputStream.getExtension())) {

                try (PDDocument document = PDDocument.load(fileInputStream.getInputStream())) {
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    PDPageTree pagesTree = document.getPages();
                    List<BufferedImage> images = new ArrayList<>(pagesTree.getCount());
                    for (int i = 0; i < pagesTree.getCount(); i++) {
                        images.add(pdfRenderer.renderImage(i));
                    }
                    return images;
                }
            }
            return Collections.singletonList(ImageIO.read(fileInputStream.getInputStream()));

        } catch (IOException e) {
            throw new RuntimeException("Unable to convert pdf to image", e);
        }
    }

    /**
     * Apply watermark - source image should already have good ratio
     *
     * @param bim source image
     * @return result image
     */
    private BufferedImage applyWatermark(BufferedImage bim) {
        try {
            BufferedImage overlay = ImageIO.read(new ClassPathResource("static/pdf/watermark-layer128dpi.png").getInputStream());

            Graphics g = bim.getGraphics();
            g.drawImage(bim, 0, 0, null);
            g.drawImage(overlay, 0, 0, bim.getWidth(), bim.getHeight(), null);
            g.dispose();

            return bim;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to fit image to the page", e);
        }
    }

    /**
     * Fit image to page dimension - A4 with 128 DPI
     *
     * @param bim
     * @return image fit to the page
     */
    private BufferedImage fitImageToPage(BufferedImage bim) {
        try {
            float ratioImage = bim.getHeight() / (float) bim.getWidth();
            float ratioPDF = params.mediaBox.getHeight() / params.mediaBox.getWidth();

            // scale according the greater axis
            PageDimension dimension = (ratioImage < ratioPDF) ?
                    new PageDimension(bim.getWidth(), (int) (bim.getWidth() * ratioPDF), 0)
                    : new PageDimension((int) (bim.getHeight() / ratioPDF), bim.getHeight(), 0);


            float scale = (dimension.width < params.maxPage.width) ? 1f :
                    params.maxPage.width / (float) bim.getWidth();// image is too big - scale if necessary

            // translate in center and scale if necessary
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.translate(scale * (dimension.width - bim.getWidth()) / 2, scale * (dimension.height - bim.getHeight()) / 2);
            affineTransform.scale(scale, scale);

            // Draw the image on to the buffered image
            BufferedImage resultImage = new BufferedImage((int) (scale * dimension.width), (int) (scale * dimension.height), BufferedImage.TYPE_INT_RGB);

            Graphics2D g = resultImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, (int) (scale * dimension.width), (int) (scale * dimension.height));
            g.drawImage(bim, affineTransform, null);
            g.dispose();

            return resultImage;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to fit image to the page", e);
        }
    }

    /**
     * Add A4 page to Document from image.
     *
     * @param document document destination
     * @param bim      image to include
     */
    private void addImageAsPageToDocument(PDDocument document, BufferedImage bim) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIOUtil.writeImage(bim, "jpg", out, params.maxPage.dpi, params.compressionQuality);


            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, out.toByteArray(), "");
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
                contentStream.drawImage(pdImage, 0, 0, PDRectangle.A4.getWidth(), bim.getHeight() * PDRectangle.A4.getWidth() / bim.getWidth());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to write image");
        }
    }

}

