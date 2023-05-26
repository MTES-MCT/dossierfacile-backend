package fr.dossierfacile.process.file.util;

import fr.dossierfacile.process.file.model.TwoDDoc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtilityTest {

    @InjectMocks
    private Utility utility;

    @Test
    void parseTwoDDoc() {
        String twoDDocString = "DC04FR04FPE3FFFF202A0401FR431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D\u001FC27GM6DXHV2PWAIZ5Q25SBOC64EH6O3IQWWYADIV3YH7ZKJ3JHACHP5EZLBZ6GP6SDE6ZYKCZHKYRRFAJ5NSV5YKO5MVGPTQDEPSZ3Y";
        TwoDDoc twoDDoc = utility.parseTwoDDoc(twoDDocString);
        Assertions.assertEquals(4, twoDDoc.getVersion());
        Assertions.assertEquals("1267027554499", twoDDoc.getFiscalNumber());
    }

}