package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.model.TwoDDoc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class UtilityTest {
    @Mock
    FileStorageService fileStorageService;

    @InjectMocks
    private Utility utility;

    @Test
    void parseTwoDDoc() {
        String twoDDocString = "DC04FR04FPE3FFFF202A0401FR431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D\u001FC27GM6DXHV2PWAIZ5Q25SBOC64EH6O3IQWWYADIV3YH7ZKJ3JHACHP5EZLBZ6GP6SDE6ZYKCZHKYRRFAJ5NSV5YKO5MVGPTQDEPSZ3Y";
        TwoDDoc twoDDoc = utility.parseTwoDDoc(twoDDocString);
        Assertions.assertEquals(4, twoDDoc.getVersion());
        Assertions.assertEquals("1267027554499", twoDDoc.getFiscalNumber());
    }

    @Test
    void extract2DDoc() throws IOException {
        StorageFile file = StorageFile.builder().build();
        Mockito.when(fileStorageService.download(file)).thenReturn(UtilityTest.class.getResourceAsStream("/2ddoc.pdf"));
        Assertions.assertEquals("DC02FR000001125E125B0126FR247500010MME/SPECIMEN/NATACHA\u001D22145 AVENUE DES SPECIMENS\u001D\u001F54LDD5F7JD4JEFPR6WZYVZVB2JZXPZB73SP7WUTN5N44P3GESXW75JZUZD5FM3G4URAJ6IKDSSUB66Y3OWQIEH22G46QOAGWH7YHJWQ",
                utility.extractTax2DDoc(file));
    }

}