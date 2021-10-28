package fr.gouv.bo.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Descriptor;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jfif.JfifDescriptor;
import com.drew.metadata.jfif.JfifDirectory;
import fr.gouv.bo.model.PdfMergeModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class Utility {
    private static final PDRectangle MINUMUM_BASE_PDF_PAGE = PDRectangle.A4;
    private static final float RATIO_MINIMUM_BASE_PDF_PAGE = PDRectangle.A4.getHeight() / PDRectangle.A4.getWidth();
    private static final float DPI_RENDERING_PDF_PAGE_TO_IMAGE = 150;

    public static void mergeAndWatermarkImage(InputStream imageIS, List<PdfMergeModel> pdfMergeModels) {
        try {
            byte[] imageBytes = transformImageOrientationIfNeeded(imageIS);
            createWatermarkedPdfPageFromImage(imageBytes, pdfMergeModels, false);
        } catch (IOException e) {
            log.error("Problem watermarking image");
            log.error(e.getMessage(), e.getCause());
        }
    }

    public static void mergeAndWatermarkPdf(InputStream pdfIS, List<PdfMergeModel> pdfMergeModels) {
        List<ByteArrayOutputStream> outputStreamList = generateListOfImagesFromPdfPages(pdfIS);
        for (ByteArrayOutputStream outputStream : outputStreamList
        ) {
            createWatermarkedPdfPageFromImage(outputStream.toByteArray(), pdfMergeModels, true);
        }
    }

    private static void createWatermarkedPdfPageFromImage(byte[] imageBytes, List<PdfMergeModel> pdfMergeModels, boolean isPdfPage) {
        log.info("Come from PDF[= true] / Image[= false] : " + isPdfPage);
        try {

            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            byte[] imageBytesAfterDisplacement = imageBytes;

            // If the image comes from an originally uploaded image and NOT from a pdf file page, then we will
            // treat its dimensions before making its page
            if (!isPdfPage) {
                // image case with smaller dimensions than base
                if (sourceImage.getWidth() < (int) MINUMUM_BASE_PDF_PAGE.getWidth()
                        || sourceImage.getHeight() < (int) MINUMUM_BASE_PDF_PAGE.getHeight()) {
                    imageBytesAfterDisplacement = createImageWithWhiteSpaceFromSmallerImage(sourceImage).toByteArray();
                } else {
                    float ratioSourceImage = (float) sourceImage.getHeight() / (float) sourceImage.getWidth();
                    // larger image case with different aspect ratio to base
                    if (ratioSourceImage != RATIO_MINIMUM_BASE_PDF_PAGE) {
                        imageBytesAfterDisplacement = createImageWithWhiteSpaceFromLargerImage(sourceImage).toByteArray();
                    }
                }
            }

            byte[] bytesFromWatermarkedImage = watermarkImage(imageBytesAfterDisplacement);

            BufferedImage watermarkedImage = ImageIO.read(new ByteArrayInputStream(bytesFromWatermarkedImage));

            PDPage page = new PDPage(new PDRectangle(watermarkedImage.getWidth(), watermarkedImage.getHeight()));
            PDDocument document = new PDDocument();
            document.addPage(page);

            PDImageXObject pdImageXObject = PDImageXObject.createFromByteArray(document, bytesFromWatermarkedImage, "");
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.drawImage(pdImageXObject, 0, 0);
            contentStream.close();
            ByteArrayOutputStream byteArrayOutputPdf = new ByteArrayOutputStream();
            document.save(byteArrayOutputPdf);
            document.close();
            pdfMergeModels.add(PdfMergeModel
                    .builder()
                    .extension("pdf")
                    .inputStream(new ByteArrayInputStream(
                            byteArrayOutputPdf.toByteArray()
                    ))
                    .build());
        } catch (IOException e) {
            log.error("Problem watermarking image");
            log.error(e.getMessage(), e.getCause());
        }
    }

    private static byte[] watermarkImage(byte[] imageBytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            Resource resource = new ClassPathResource("static/pdf/watermark-big.png");
            BufferedImage layerWatermarkImage = ImageIO.read(resource.getInputStream());

            //Scaling layout image of watermark.
            Image scaledWatermarkLayout = layerWatermarkImage.getScaledInstance(sourceImage.getWidth(), sourceImage.getHeight(), Image.SCALE_AREA_AVERAGING);

            BufferedImage finalImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D graphics2D = finalImage.createGraphics();
            graphics2D.drawImage(sourceImage, 0, 0, null);
            graphics2D.drawImage(scaledWatermarkLayout, 0, 0, null);
            graphics2D.dispose();

            ImageIO.write(finalImage, "png", outputStream);
        } catch (IOException e) {
            log.error("Problem creating the merged image");
            log.error(e.getMessage(), e.getCause());
        }
        return outputStream.toByteArray();
    }

    private static ByteArrayOutputStream createImageWithWhiteSpaceFromSmallerImage(BufferedImage sourceImage) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();

        float ratioSourceImage = (float) sourceImage.getHeight() / (float) sourceImage.getWidth();
        float newWidth = 0;
        float newHeight = 0;
        if (ratioSourceImage < RATIO_MINIMUM_BASE_PDF_PAGE) {
            newWidth = Math.max(sourceImage.getWidth(), MINUMUM_BASE_PDF_PAGE.getWidth());
            newHeight = newWidth * RATIO_MINIMUM_BASE_PDF_PAGE;
        } else if (ratioSourceImage > RATIO_MINIMUM_BASE_PDF_PAGE) {
            newHeight = Math.max(sourceImage.getHeight(), MINUMUM_BASE_PDF_PAGE.getHeight());
            newWidth = newHeight / RATIO_MINIMUM_BASE_PDF_PAGE;
        }

        BufferedImage finalImage = new BufferedImage((int) newWidth, (int) newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = finalImage.createGraphics();

        int[] imageDisplacement = imageDisplacement(sourceImage.getWidth(), sourceImage.getHeight(), newWidth, newHeight);
        graphics2D.drawImage(sourceImage, imageDisplacement[0], imageDisplacement[1], null);
        graphics2D.dispose();

        try {
            ImageIO.write(finalImage, "png", bo);
        } catch (IOException e) {
            log.error("Problem creating image with white space for smaller image than base.");
            log.error(e.getMessage(), e.getCause());
        }

        return bo;
    }

    private static ByteArrayOutputStream createImageWithWhiteSpaceFromLargerImage(BufferedImage sourceImage) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();

        float ratioSourceImage = (float) sourceImage.getHeight() / (float) sourceImage.getWidth();
        float newWidth = 0;
        float newHeight = 0;
        if (ratioSourceImage < RATIO_MINIMUM_BASE_PDF_PAGE) {
            newWidth = sourceImage.getWidth();
            newHeight = newWidth * RATIO_MINIMUM_BASE_PDF_PAGE;
        } else if (ratioSourceImage > RATIO_MINIMUM_BASE_PDF_PAGE) {
            newHeight = sourceImage.getHeight();
            newWidth = newHeight / RATIO_MINIMUM_BASE_PDF_PAGE;
        }

        BufferedImage finalImage = new BufferedImage((int) newWidth, (int) newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = finalImage.createGraphics();

        int[] imageDisplacement = imageDisplacement(sourceImage.getWidth(), sourceImage.getHeight(), newWidth, newHeight);
        graphics2D.drawImage(sourceImage, imageDisplacement[0], imageDisplacement[1], null);
        graphics2D.dispose();

        try {
            ImageIO.write(finalImage, "png", bo);
        } catch (IOException e) {
            log.error("Problem creating image with white space for larger image than base.");
            log.error(e.getMessage(), e.getCause());
        }

        return bo;
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

    public static ByteArrayOutputStream mergePdfModels(List<PdfMergeModel> pdfMergeModels) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PDFMergerUtility ut = new PDFMergerUtility();
        for (PdfMergeModel pdfMergeModel : pdfMergeModels) {
            if (pdfMergeModel.getExtension().equals("pdf")) {
                ut.addSource(pdfMergeModel.getInputStream());
            } else {
                ut.addSource(
                        new ByteArrayInputStream(
                                convertImgToPDF(pdfMergeModel.getInputStream())
                        ));
            }
        }
        try {
            ut.setDestinationStream(outputStream);
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error("Exception while trying to merge documents", e);
        }
        return outputStream;
    }

    private static byte[] convertImgToPDF(InputStream inputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PDDocument document = new PDDocument()) {
            byte[] inputBytes = IOUtils.toByteArray(inputStream);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
            if (bufferedImage != null) {
                float width = bufferedImage.getWidth();
                float height = bufferedImage.getHeight();

                int[] imageDisplacement = imageDisplacement(bufferedImage.getWidth(), bufferedImage.getHeight());

                PDPage page = new PDPage(MINUMUM_BASE_PDF_PAGE);
                document.addPage(page);
                PDImageXObject img = PDImageXObject.createFromByteArray(document, inputBytes, "");
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                contentStream.drawImage(img, imageDisplacement[0], imageDisplacement[1], width, height);

                contentStream.close();
                document.save(baos);
            }
        } catch (IOException e) {
            log.error("Exception while trying to convert image to pdf", e);
        }
        return baos.toByteArray();
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

    private static double angle(double a, double b, double c) {
        return Math.acos((Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2 * a * b));
    }

    private static List<ByteArrayOutputStream> generateListOfImagesFromPdfPages(InputStream inputStream) {

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

    private static int[] imageDisplacement(float sourceWidth, float sourceHeight) {
        return imageDisplacement(sourceWidth, sourceHeight, MINUMUM_BASE_PDF_PAGE.getWidth(), MINUMUM_BASE_PDF_PAGE.getHeight());
    }

    private static int[] imageDisplacement(float sourceWidth, float sourceHeight, float newWidth, float newHeight) {
        //index "0" will contain the horizontal displacement
        //index "1" will contain the vertical displacement
        int[] displacementXY = new int[2];

        if (sourceWidth < newWidth) {
            float hDispl = (newWidth - sourceWidth) / 2;
            displacementXY[0] = (int) hDispl;
        }
        if (sourceHeight < newHeight) {
            float vDispl = (newHeight - sourceHeight) / 2;
            displacementXY[1] = (int) vDispl;
        }
        return displacementXY;
    }

    private static byte[] transformImageOrientationIfNeeded(InputStream inputStream) throws IOException {
        byte[] inputBytes = IOUtils.toByteArray(inputStream);
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
        int orientation = extractMetadataOrientation(new ByteArrayInputStream(inputBytes));
        if (orientation != 1) {
            AffineTransform affineTransform = getExifTransformation(orientation, bufferedImage.getWidth(), bufferedImage.getHeight());
            return transformImageOrientation(bufferedImage, affineTransform).toByteArray();
        } else {
            return inputBytes;
        }
    }

    private static ByteArrayOutputStream transformImageOrientation(BufferedImage image, AffineTransform transform) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);

        BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null);
        Graphics2D g = destinationImage.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
        destinationImage = op.filter(image, destinationImage);
        ImageIO.write(destinationImage, "png", outputStream);
        return outputStream;
    }

    private static int extractMetadataOrientation(InputStream inputStream) {
        int orientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            JfifDirectory jfifDirectory = metadata.getFirstDirectoryOfType(JfifDirectory.class);
            if (jfifDirectory != null && (jfifDirectory.containsTag(JfifDirectory.TAG_RESX) || jfifDirectory.containsTag(JfifDirectory.TAG_RESY))) {
                JfifDescriptor descriptor = new JfifDescriptor(jfifDirectory);
                log.info("Horizontal resolution : " + descriptor.getImageResXDescription() + " in " + descriptor.getImageResUnitsDescription());
                log.info("Vertical resolution : " + descriptor.getImageResYDescription() + " in " + descriptor.getImageResUnitsDescription());
            }

            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                Integer orientationValue = exifIFD0Directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
                if (orientationValue != null) {
                    orientation = orientationValue;
                }
                log.info("Original orientation : " + new ExifIFD0Descriptor(exifIFD0Directory).getOrientationDescription());
            }
        } catch (ImageProcessingException | IOException e) {
            log.error("Problem extracting metadata information of stream");
            log.error(e.getMessage(), e.getCause());
        }
        return orientation;
    }

    private static AffineTransform getExifTransformation(int orientation, int width, int height) {
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 1:
                break;
            case 2: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-width, 0);
                break;
            case 3: // PI rotation
                t.translate(width, height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -height);
                break;
            case 5: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case 6: // -PI/2 and -width
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-height, 0);
                t.translate(0, width);
                t.rotate(3 * Math.PI / 2);
                break;
            case 8: // PI / 2
                t.translate(0, width);
                t.rotate(3 * Math.PI / 2);
                break;
        }
        return t;
    }
}
