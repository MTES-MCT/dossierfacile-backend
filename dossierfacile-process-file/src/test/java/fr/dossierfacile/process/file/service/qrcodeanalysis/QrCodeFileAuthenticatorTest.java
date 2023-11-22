package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.PayfitDocumentIssuer;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QrCodeFileAuthenticatorTest {

    private final PayfitClient client = mock(PayfitClient.class);
    private final QrCodeFileAuthenticator authenticator = new QrCodeFileAuthenticator(List.of(new PayfitDocumentIssuer(client)));

    @Test
    void should_process_pdf_file() throws IOException {
        Optional<BarCodeFileAnalysis> result = authenticator.analyze(TestFilesUtil.getPdfFile("fake-payfit.pdf"));
        assertThat(result).isPresent();
        assertThat(result.get().getDocumentType()).isEqualTo(BarCodeDocumentType.PAYFIT_PAYSLIP);
    }

}