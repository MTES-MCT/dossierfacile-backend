package fr.dossierfacile.process.file;

import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;

public class TestFilesUtil {

    private static final ClassLoader CLASS_LOADER = TestFilesUtil.class.getClassLoader();

    public static InputStream getFileAsStream(String fileName) {
        String path = "documents/" + fileName;
        return CLASS_LOADER.getResourceAsStream(path);
    }

    public static InMemoryPdfFile getPdfFile(String fileName) throws IOException {
        try (InputStream inputStream = getFileAsStream(fileName)) {
            return new InMemoryPdfFile(PDDocument.load(inputStream));
        }
    }

}
