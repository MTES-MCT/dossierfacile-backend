package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static fr.dossierfacile.process.file.TestFilesUtil.getFilesFromDirectory;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// Disable this test because the dataSources are not available for the moment
@Disabled
public class TwoDDocParserTest {

    record ParameterTest(String fileName, boolean isValid) {
    }

    private static Stream<Arguments> provideTwoDDocParameters() throws URISyntaxException, IOException {
        var validFiles = getFilesFromDirectory(Path.of("2dDocDatasource", "valid"));
        var invalidFiles = getFilesFromDirectory(Path.of("2dDocDatasource", "invalid"));
        return Stream.concat(validFiles.stream(), invalidFiles.stream()).map(
                file -> Arguments.of(Named.of(file.toString(), new ParameterTest(file.toString(), !invalidFiles.contains(file))))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTwoDDocParameters")
    void test2dParsingOnDocuments(ParameterTest parameterTest) throws IOException {
        if (parameterTest.fileName.contains("pdf")) {
            var pdfDocument = TestFilesUtil.getPdfBoxDocument(parameterTest.fileName);
            Optional<TwoDDocRawContent> actualContent = TwoDDocPdfFinder.on(pdfDocument).find2DDoc();
            if (parameterTest.isValid) {
                assertThat(actualContent).isPresent();
                assertThat(actualContent.get().rawContent()).isNotEmpty();
            } else {
                assertThat(actualContent).isEmpty();
            }
        } else {
            BufferedImage image = TestFilesUtil.getImage(parameterTest.fileName);
            Optional<TwoDDocRawContent> actualContent = new TwoDDocImageFinder(image).find2DDoc();
            if (parameterTest.isValid) {
                assertThat(actualContent).isPresent();
                assertThat(actualContent.get().rawContent()).isNotEmpty();
            } else {
                assertThat(actualContent).isEmpty();
            }
        }
    }

}
