package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TwoDDocImageFinderTest {

    @Test
    void should_read_2DDoc_on_image() throws IOException {
        Optional<TwoDDocRawContent> actualContent = findOnImage("2ddoc.jpg");
        TwoDDocRawContent expectedContent = new TwoDDocRawContent("DC02FR000001125E125B0126FR247500010MME/SPECIMEN/NATACHA\u001D22145 AVENUE DES SPECIMENS\u001D\u001F54LDD5F7JD4JEFPR6WZYVZVB2JZXPZB73SP7WUTN5N44P3GESXW75JZUZD5FM3G4URAJ6IKDSSUB66Y3OWQIEH22G46QOAGWH7YHJWQ");
        assertThat(actualContent).isPresent().contains(expectedContent);
    }

    @Test
    void should_return_empty_if_no_2DDoc_found() throws IOException {
        Optional<TwoDDocRawContent> twoDDoc = findOnImage("qr-code.jpg");
        assertThat(twoDDoc).isEmpty();
    }

    private static Optional<TwoDDocRawContent> findOnImage(String fileName) throws IOException {
        BufferedImage image = TestFilesUtil.getImage(fileName);
        return new TwoDDocImageFinder(image).find2DDoc();
    }

}