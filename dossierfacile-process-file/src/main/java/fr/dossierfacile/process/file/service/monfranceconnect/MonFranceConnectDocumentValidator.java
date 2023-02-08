package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.Utility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class MonFranceConnectDocumentValidator {

    private final MonFranceConnectClient mfcClient;
    private final Utility utility;
    private final ApiTesseract apiTesseract;

    public List<MonFranceConnectDocument> validate(List<File> files) {
        return files.stream()
                .filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf"))
                .map(this::validateDocumentIfMonFranceConnect)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<MonFranceConnectDocument> validateDocumentIfMonFranceConnect(File file) {
        return utility.extractQRCodeInfo(file)
                .flatMap(qrCodeUrl -> mfcClient.fetchDocumentContent(qrCodeUrl)
                        .map(content -> buildDocument(file, content)));
    }

    private MonFranceConnectDocument buildDocument(File file, DocumentVerifiedContent content) {
        boolean isDocumentValid = isActualContentMatchingWithVerifiedContent(file, content);
        return new MonFranceConnectDocument(file, content, isDocumentValid);
    }

    private boolean isActualContentMatchingWithVerifiedContent(File pdf, DocumentVerifiedContent content) {
        List<String> listResponse = content.getElements();

        String result = utility.extractInfoFromPDFFirstPage(pdf);
        AtomicInteger i = new AtomicInteger();
        if (!listResponse.isEmpty()) {
            listResponse.forEach(element -> {
                if (result.contains(element)) {
                    i.getAndIncrement();
                }
            });
            if (listResponse.size() == i.get()) {
                log.info("QR content VALID for PDF with ID [" + pdf.getId() + "]");
                return true;
            } else {
                java.io.File tmpFile = utility.getTemporaryFile(pdf);
                String tesseractResult = apiTesseract.extractText(tmpFile);
                try {
                    if (!tmpFile.delete()) {
                        log.warn("Unable to delete file");
                    }
                } catch (Exception e) {
                    log.warn("Unable to delete file", e);
                }

                AtomicInteger ii = new AtomicInteger();
                listResponse.forEach(element -> {
                    if (tesseractResult.contains(element)) {
                        ii.getAndIncrement();
                    }
                });
                if (listResponse.size() == ii.get()) {
                    log.info("QR content VALID for PDF with ID [" + pdf.getId() + "]");
                    return true;
                } else {
                    log.warn("QR content NOT VALID for the PDF with ID [" + pdf.getId() + "]");
                    return false;
                }
            }
        }
        return false;
    }

}
