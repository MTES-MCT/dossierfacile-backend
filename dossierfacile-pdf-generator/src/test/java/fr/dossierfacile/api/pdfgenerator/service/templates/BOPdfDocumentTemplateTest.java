package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.configuration.FeatureFlipping;
import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.any;

// Used for manual testing
@Disabled
@ExtendWith(MockitoExtension.class)
public class BOPdfDocumentTemplateTest {

    @Mock
    MessageSource messageSource;
    @Mock
    FeatureFlipping featureFlipping;
    @InjectMocks
    BOPdfDocumentTemplate boPdfDocumentTemplate;

    File outputfile;

    @BeforeEach
    void init() {
        Mockito.lenient().when(messageSource.getMessage(any(),any(),any(),any() )).thenReturn(BOPdfDocumentTemplate.DEFAULT_WATERMARK);
        Mockito.when(featureFlipping.shouldUseColors()).thenReturn(true);
        Mockito.when(featureFlipping.shouldUseDistortion()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (outputfile != null) {
            outputfile.delete();
        }
    }

    @DisplayName("Check if the pdf file is correctly generated in specific files")
    @Test
    public void check_render_with_special_files() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("secret/TestBOFile1.pdf");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(is)
                .build();

        File resultFile = new File("target/resultSpecial.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Collections.singletonList(data)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }


    @DisplayName("Check if the render is correctly generated from text pdf and wrong sized pdf")
    @Test
    public void check_render_with_text_pdf() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("bigH.pdf");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(is)
                .build();

        File resultFile = new File("target/resultTestPdfWrongSize.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Collections.singletonList(data)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }

    @DisplayName("Check if the render is correctly generated from all type textual pdf, pdf, image, non obfuscable pdf")
    @Test
    public void check_render_with_all_type() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.pdf");
        InputStream isJPG = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.jpg");
        InputStream isTextPdf = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("page1.pdf");
        InputStream isOpen = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.pdf");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(is)
                .build();
        FileInputStream data2 = FileInputStream
                .builder()
                .mediaType(MediaType.IMAGE_JPEG)
                .inputStream(isJPG)
                .build();
        FileInputStream data3 = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(isTextPdf)
                .build();
        FileInputStream data4 = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(isOpen)
                .build();

        File resultFile = new File("target/resultFullTypeTestPdf.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Arrays.asList(data, data2, data3, data4)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }

    @DisplayName("Check if the render is correctly generated from image pdf")
    @Test
    public void check_render_with_img_pdf() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.pdf");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(is)
                .build();

        File resultFile = new File("target/resultTestPdfWithJpeg.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Collections.singletonList(data)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }

    @DisplayName("Check if the render is correctly generated from jpegs")
    @Test
    public void check_render_from_jpegs() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.jpg");
        InputStream is2 = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNIHorizontale.jpg");
        InputStream is3 = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNISmall.jpg");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.IMAGE_JPEG)
                .inputStream(is)
                .build();
        FileInputStream data2 = FileInputStream
                .builder()
                .mediaType(MediaType.IMAGE_JPEG)
                .inputStream(is2)
                .build();
        FileInputStream data3 = FileInputStream
                .builder()
                .mediaType(MediaType.IMAGE_JPEG)
                .inputStream(is3)
                .build();
        File resultFile = new File("target/resultTestJpeg.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Arrays.asList(data, data2, data3)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }

    @DisplayName("Render watermark (used mostly for developing)")
    @Test
    public void check_watermark_rendered() throws IOException {
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("CNI.jpg");
        BufferedImage image = ImageIO.read(is);
        BufferedImage b = boPdfDocumentTemplate.applyWatermark(image, "watermark 2023");
        outputfile = new File("image.jpg");
        ImageIO.write(b, "jpg", outputfile);
    }

    @DisplayName("Avoid render above qrcode")
    @Test
    public void check_watermark_not_in_qrcode() throws IOException {
        Mockito.when(featureFlipping.shouldUseColors()).thenReturn(false);
        Mockito.when(featureFlipping.shouldUseDistortion()).thenReturn(false);
        InputStream is = BOPdfDocumentTemplateTest.class.getClassLoader().getResourceAsStream("qrcode-sample.pdf");

        FileInputStream data = FileInputStream
                .builder()
                .mediaType(MediaType.APPLICATION_PDF)
                .inputStream(is)
                .build();

        File resultFile = new File("target/resultTestPdfWithQrCode.pdf");
        resultFile.createNewFile();

        byte[] bytes = IOUtils.toByteArray(boPdfDocumentTemplate.render(Collections.singletonList(data)));

        FileOutputStream w = new FileOutputStream(resultFile);
        w.write(bytes);
    }
}
