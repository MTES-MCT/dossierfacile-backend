package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.test.utils.SwiftObjectMock;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.service.interfaces.OvhService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DownloadServiceTest {

    @Mock
    OvhService ovhService;

    @InjectMocks
    DownloadServiceImpl downloadService;


    @Test
    void test() throws IOException {
        String url = "http://localhost:8080/api/document/resource/8d19767c-8301-4a81-8f85-8957ca11e85c.pdf";

        Mockito.when(ovhService.get(url)).thenReturn(new SwiftObjectMock(url, "/CNI.pdf"));

        InputStream is = downloadService.getDocumentInputStream(Document.builder().name("doc").id(1L).name(url).build());

        byte[] waitingResult = DownloadServiceTest.class.getResourceAsStream("/CNI.pdf").readAllBytes();
        byte[] result = is.readAllBytes();

        Assertions.assertArrayEquals(waitingResult, result);
    }

}
