package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.service.DownloadServiceImpl;
import fr.dossierfacile.api.pdfgenerator.util.parameterresolvers.ApartmentSharingResolver;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.logging.job.LogAggregator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@ExtendWith(ApartmentSharingResolver.class)
public class ApartmentSharingPdfDocumentTemplateTest {

    @MockitoBean
    private TenantCommonRepository tenantRepository;
    @MockitoBean
    private DownloadServiceImpl downloadService;
    @MockitoBean
    private LogAggregator logAggregator;
    @MockitoBean
    private MailCommonService mailCommonService;
    @Autowired
    private ApartmentSharingPdfDocumentTemplate pdfService;

    @BeforeEach
    void init_mocks() {
        Mockito.when(tenantRepository.countTenantsBlockingFullPdfGeneration(anyLong())).thenReturn(0);
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

    // A COMPLETED dossier renders with the dedicated first-page template
    // (visual check: target/fullPdfGenerationCompleted.pdf)
    @Test
    void should_generate_pdf_for_completed_dossier(ApartmentSharing apartmentSharing) throws IOException {
        apartmentSharing.getTenants().forEach(tenant -> tenant.setStatus(TenantFileStatus.COMPLETED));
        File resultFile = new File("target/fullPdfGenerationCompleted.pdf");

        try (FileOutputStream w = new FileOutputStream(resultFile); InputStream is = pdfService.render(apartmentSharing)) {
            byte[] result = is.readAllBytes();
            w.write(result);
            Assertions.assertThat(result).isNotEmpty();
        }
    }

}