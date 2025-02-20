package fr.dossierfacile.process.file;

import fr.dossierfacile.process.file.barcode.InMemoryPdfFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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

    public static List<String> getFilesFromDirectory(Path directory) throws URISyntaxException, IOException {
        var folderPath = Path.of("documents", directory.toString()).toString();
        var url = CLASS_LOADER.getResource(folderPath);

        if (url == null) {
            throw new IllegalArgumentException("folder does not exist : " + folderPath);
        }

        var actualPath = Paths.get(url.toURI());

        return Files.list(actualPath)
                .map(file -> {
                    return directory + "/" + file.getFileName().toString();
                })
                .collect(Collectors.toList());

    }

}
