package fr.dossierfacile.api.pdfgenerator.service.templates;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.twelvemonkeys.image.ImageUtil;
import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.model.PageDimension;
import fr.dossierfacile.api.pdfgenerator.model.PdfTemplateParameters;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@AllArgsConstructor
@Slf4j
@Primary
public class BOPdfDocumentTemplate implements PdfTemplate<List<FileInputStream>> {
    public static final String DEFAULT_WATERMARK = "  DOCUMENTS EXCLUSIVEMENT DESTIN\u00c9S \u00c0 LA LOCATION IMMOBILI\u00c8RE     ";
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private final PdfTemplateParameters params = PdfTemplateParameters.builder().build();
    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;

    private static ConvolveOp getGaussianBlurFilter(int radius, boolean horizontal) {
        int size = radius * 2 + 1;
        float[] data = new float[size];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }
        Kernel kernel = (horizontal) ? new Kernel(size, 1, data) : new Kernel(1, size, data);

        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    @Override
    public InputStream render(List<FileInputStream> data) throws IOException {
        return this.render(data,
                messageSource.getMessage("tenant.pdf.watermark", null, DEFAULT_WATERMARK, locale));
    }

    public InputStream render(List<FileInputStream> data, String watermarkText) throws IOException {
        final String watermarkToApply = StringUtils.isNotBlank(watermarkText) ? watermarkText + "   " :
                messageSource.getMessage("tenant.pdf.watermark.default", null, " https://filigrane.beta.gouv.fr/   ", locale);

        try (PDDocument document = new PDDocument()) {

            data.stream()
                    .map(this::convertToImages)
                    .flatMap(Collection::stream)
                    .map(this::smartCrop)
                    .filter(Objects::nonNull)
                    .map(this::fitImageToPage)
                    .map(bim -> applyWatermark(bim, watermarkToApply))
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

            if (MediaType.APPLICATION_PDF.equalsTypeAndSubtype(fileInputStream.getMediaType())) {
                List<BufferedImage> images = new ArrayList<>();
                try (PDDocument document = PDDocument.load(fileInputStream.getInputStream())) {
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    PDPageTree pagesTree = document.getPages();
                    for (int i = 0; i < pagesTree.getCount(); i++) {
                        PDRectangle pageMediaBox = pagesTree.get(i).getMediaBox();
                        float ratioImage = pageMediaBox.getHeight() / pageMediaBox.getWidth();
                        float ratioPDF = params.mediaBox.getHeight() / params.mediaBox.getWidth();

                        // scale according the greater axis
                        PageDimension dimension = (ratioImage < ratioPDF) ?
                                new PageDimension((int) pageMediaBox.getWidth(), (int) (pageMediaBox.getWidth() * ratioPDF), 0)
                                : new PageDimension((int) (pageMediaBox.getHeight() / ratioPDF), (int) pageMediaBox.getHeight(), 0);


                        float scale = (dimension.width < params.maxPage.width) ? 1f :
                                params.maxPage.width / pageMediaBox.getWidth();// image is too big - scale if necessary

                        // x2 - double the image resolution (prevent quality loss if image is cropped)
                        images.add(pdfRenderer.renderImage(i, scale * 2, ImageType.RGB));
                    }
                    return images;
                } catch (Exception e) {
                    log.error("Exception while converting pdf page to image", e);
                    log.error(EXCEPTION + Sentry.captureException(e));
                    return images;
                }
            }

            return Collections.singletonList(createImageWithOrientation(fileInputStream.getInputStream()));

        } catch (IOException e) {
            throw new RuntimeException("Unable to convert pdf to image", e);
        }
    }

    private BufferedImage createImageWithOrientation(InputStream inputStream) throws IOException {
        // duplicate input stream - because it will be read twice
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, baos);
        byte[] bytes = baos.toByteArray();

        try (ByteArrayInputStream imageInput = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(imageInput);

            // check en rotate according metadata
            try (ByteArrayInputStream imageInputForMeta = new ByteArrayInputStream(bytes)) {

                Metadata metadata = ImageMetadataReader.readMetadata(imageInputForMeta);
                Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                int orientation = dir != null && dir.getInteger(ExifIFD0Directory.TAG_ORIENTATION) != null
                        ? dir.getInt(ExifIFD0Directory.TAG_ORIENTATION) : 0;

                return switch (orientation) {
                    case 2 -> ImageUtil.createFlipped(image, ImageUtil.FLIP_HORIZONTAL);
                    case 3 -> ImageUtil.createRotated(image, ImageUtil.ROTATE_180);
                    case 4 -> ImageUtil.createFlipped(image, ImageUtil.FLIP_VERTICAL);
                    case 5 -> ImageUtil.createRotated(
                            ImageUtil.createFlipped(image, ImageUtil.FLIP_VERTICAL),
                            ImageUtil.ROTATE_90_CW);
                    case 6 -> ImageUtil.createRotated(image, ImageUtil.ROTATE_90_CW);
                    case 7 -> ImageUtil.createRotated(
                            ImageUtil.createFlipped(image, ImageUtil.FLIP_VERTICAL),
                            ImageUtil.ROTATE_90_CCW);
                    case 8 -> ImageUtil.createRotated(image, ImageUtil.ROTATE_90_CCW);
                    default -> image; // 0,1 included
                };

            } catch (Exception e) {
                log.error("Unable to rotate and flip from metadata", e);
                Sentry.captureException(e);
            }
            return image;
        }
    }

    /**
     * Apply document crop if require
     */
    protected BufferedImage smartCrop(BufferedImage image) {
        // by default there is not crop on BO Documents
        return image;
    }

    /**
     * Apply watermark - source image should already have good ratio
     *
     * @param bim source image
     * @return result image
     */
    private BufferedImage applyWatermark(BufferedImage bim, String watermarkText) {
        try {
            //Create a watermark layer
            BufferedImage watermarkLayer = new BufferedImage(bim.getWidth(), bim.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = watermarkLayer.createGraphics();

            String watermark = watermarkText.repeat(1 + (128 / watermarkText.length()));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ThreadLocalRandom.current().nextFloat(0.52f, 0.6f)));
            g.setColor(Color.DARK_GRAY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font font = new Font("Arial", Font.PLAIN, 28 * bim.getWidth() / params.maxPage.width);
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(-Math.PI / 4f, 0, 0);
            Font rotatedFont = font.deriveFont(affineTransform);
            g.setFont(rotatedFont);

            // allows to have small variation on the watermark position at each generation
            float spaceBetweenText = bim.getHeight() / ThreadLocalRandom.current().nextFloat(5f, 6f);
            for (int i = 1; i < 10; i++) {
                g.drawString(watermark, 0, i* spaceBetweenText);
            }

            // Create a gaussian blur layer
            int radius = ThreadLocalRandom.current().nextInt(45, 65);

            BufferedImage blurredTextLayer = new BufferedImage(bim.getWidth(), bim.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D blurredTextLayerGraphics = blurredTextLayer.createGraphics();
            blurredTextLayerGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ThreadLocalRandom.current().nextFloat(0.75f, 0.95f)));
            blurredTextLayerGraphics.drawImage(watermarkLayer, 0, 0, null);
            blurredTextLayer = getGaussianBlurFilter(radius, true).filter(blurredTextLayer, null);
            blurredTextLayer = getGaussianBlurFilter(radius, false).filter(blurredTextLayer, null);
            blurredTextLayerGraphics.dispose();

            // Merge layers
            Graphics2D gf = bim.createGraphics();
            gf.drawImage(bim, 0, 0, null);
            gf.drawImage(blurredTextLayer, 0, 0, null);
            gf.drawImage(watermarkLayer, 0, 0, null);
            gf.dispose();

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

