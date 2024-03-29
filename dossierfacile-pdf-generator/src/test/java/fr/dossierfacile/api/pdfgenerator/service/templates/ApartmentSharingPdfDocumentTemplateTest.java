package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.service.DownloadServiceImpl;
import fr.dossierfacile.api.pdfgenerator.util.parameterresolvers.ApartmentSharingResolver;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@ExtendWith(ApartmentSharingResolver.class)
public class ApartmentSharingPdfDocumentTemplateTest {

    @MockBean
    private TenantCommonRepository tenantRepository;

    @MockBean
    private DownloadServiceImpl downloadService;

    @Autowired
    private ApartmentSharingPdfDocumentTemplate pdfService;

    @BeforeEach
    void init_mocks() {
        Mockito.when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);
        Mockito.when(downloadService.getDocumentInputStream(any())).then(answer -> ApartmentSharingPdfDocumentTemplateTest.class.getResourceAsStream("/CNI.pdf"));
    }

    @Test
    void should_generate_pdf(ApartmentSharing apartmentSharing) throws IOException {
        File resultFile = new File("target/fullPdfGeneration.pdf");

        try (FileOutputStream w = new FileOutputStream(resultFile); InputStream is = pdfService.render(apartmentSharing)) {
            byte[] result = is.readAllBytes();
            w.write(result);
            Assertions.assertThat(result).isNotEmpty();
        }
    }

}
