package fr.dossierfacile.process.file;

import fr.dossierfacile.process.file.barcode.InMemoryPdfFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class TestFilesUtil {

    private static final ClassLoader CLASS_LOADER = TestFilesUtil.class.getClassLoader();

    public static InputStream getFileAsStream(String fileName) {
        String path = "documents/" + fileName;
        return CLASS_LOADER.getResourceAsStream(path);
    }

    public static InMemoryPdfFile getPdfFile(String fileName) throws IOException {
        return new InMemoryPdfFile(getPdfBoxDocument(fileName));
    }

    public static PDDocument getPdfBoxDocument(String fileName) throws IOException {
        try (InputStream inputStream = getFileAsStream(fileName)) {
            return Loader.loadPDF(inputStream.readAllBytes());
        }
    }

    public static BufferedImage getImage(String fileName) throws IOException {
        try (InputStream inputStream = getFileAsStream(fileName)) {
            return ImageIO.read(inputStream);
        }
    }

}
