package fr.dossierfacile.api.pdfgenerator.service.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.pdfgenerator.service.DownloadServiceTest;
import fr.dossierfacile.api.pdfgenerator.service.DownloadServiceImpl;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.io.*;

import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ApartmentSharingPdfDocumentTemplateTest {

    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private MessageSource messageSource;

    @Mock
    private DownloadServiceImpl downloadService;

    @InjectMocks
    private ApartmentSharingPdfDocumentTemplate pdfService;


    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void init_mocks() throws IOException {
        Mockito.when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);
        Mockito.when(messageSource.getMessage(anyString(), any(), any())).thenAnswer(answer -> answer.getArguments()[0]);
        Mockito.when(downloadService.getDocumentInputStream(any())).then(answer -> DownloadServiceTest.class.getResourceAsStream("/CNI.pdf"));

        this.apartmentSharing = loadApartmentSharing("/apartmentSharing.json");
    }

    private ApartmentSharing loadApartmentSharing(String jsonFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = ApartmentSharingPdfDocumentTemplateTest.class.getResourceAsStream(jsonFilePath);
        ApartmentSharing apartmentSharing = mapper.readValue(is, ApartmentSharing.class);
        return apartmentSharing;
    }


    //@Test
    void test() throws IOException {
        File resultFile = new File("target/fullPdfGeneration.pdf");
        resultFile.createNewFile();

        try (FileOutputStream w = new FileOutputStream(resultFile)) {
            InputStream is = pdfService.render(apartmentSharing);
            byte[] result = is.readAllBytes();
            w.write(result);
            Assertions.assertThat(result.length > 0);
        }
    }


}
