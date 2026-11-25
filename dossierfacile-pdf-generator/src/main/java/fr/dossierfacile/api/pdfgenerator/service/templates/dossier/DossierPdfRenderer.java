package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfSignatureService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.TenantLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Renders the PDF of a dossier. {@link DossierPdfStatus} chooses the template, in {@code templates/dossier-pdf/}.
 * <p>
 * The HTML engine lays out the same content twice. The first time, {@link DossierPdfMeasurer} only reads the heights of
 * the blocks, and {@link DossierPdfLayout} then spreads the summary and sizes the images. The second time, the engine
 * prints the PDF. {@code dossier.css} alone defines the dimensions.
 * <p>
 * This class uses PDFBox to read the pieces and to sign the result, and for nothing else.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DossierPdfRenderer {

    static final String QUESTIONNAIRE_URL = "https://dossierfacile.fr/questionnaire";
    private static final int QR_CODE_SIZE = 280;

    private final DownloadService downloadService;
    private final TenantLogRepository tenantLogRepository;
    private final PdfSignatureService pdfSignatureService;
    private final PieceExtractor pieceExtractor;
    private final DossierHtmlEngine htmlEngine = new DossierHtmlEngine();
    private final DossierPdfMeasurer measurer = new DossierPdfMeasurer(htmlEngine);

    public InputStream render(ApartmentSharing apartmentSharing) throws IOException {
        DossierPdfResources resources = new DossierPdfResources();
        DossierPdfStatus status = DossierPdfStatus.of(apartmentSharing);
        LocalDate date = status == DossierPdfStatus.VALIDATED ? validationDate(apartmentSharing) : depositDate(apartmentSharing);
        DossierPdfModel model = new DossierPdfModelBuilder(resources, this::pieceContents).build(apartmentSharing, status, date);
        DossierPdfLayout layout = DossierPdfLayout.of(model, measurer.measure(model, resources));
        Map<String, Object> variables = Map.of(
                "m", model,
                "layout", layout,
                "qrCode", resources.add("qr-code.png", qrCode()),
                "questionnaireUrl", QUESTIONNAIRE_URL);
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        htmlEngine.prepare(status.getTemplate(), variables, resources, pdf).run();

        ByteArrayOutputStream signed = new ByteArrayOutputStream();
        try (PDDocument document = Loader.loadPDF(pdf.toByteArray())) {
            pdfSignatureService.signAndSaveKeepingDescription(document, signed);
        } catch (Exception e) {
            throw new IOException("Unable to sign the dossier PDF of apartment sharing " + apartmentSharing.getId(), e);
        }
        log.info("Dossier PDF {} generated for apartment sharing {} ({} piece pages)", status, apartmentSharing.getId(), model.getPiecePages().size());
        return new ByteArrayInputStream(signed.toByteArray());
    }

    private List<PieceContent> pieceContents(Document document) {
        try (InputStream pdf = downloadService.getDocumentInputStream(document)) {
            return pieceExtractor.extract(pdf);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the watermarked file of document " + document.getId(), e);
        }
    }

    /**
     * Returns the validation date of the dossier, which is the last validation among its tenants.
     */
    private LocalDate validationDate(ApartmentSharing apartmentSharing) {
        return lastLogDate(apartmentSharing, LogType.ACCOUNT_VALIDATED, apartmentSharing.getTenants().stream().map(Tenant::getOperatorDateTime));
    }

    /**
     * Returns the date of the last submission among the tenants, or else the date of the last document.
     */
    private LocalDate depositDate(ApartmentSharing apartmentSharing) {
        return lastLogDate(apartmentSharing, LogType.ACCOUNT_COMPLETED, apartmentSharing.getTenants().stream()
                .flatMap(tenant -> Stream.concat(tenant.getDocuments().stream(), tenant.getGuarantors().stream().flatMap(g -> g.getDocuments().stream())))
                .map(Document::getCreationDateTime));
    }

    private LocalDate lastLogDate(ApartmentSharing apartmentSharing, LogType logType, Stream<LocalDateTime> fallbacks) {
        List<Long> tenantIds = apartmentSharing.getTenants().stream().map(Tenant::getId).filter(Objects::nonNull).toList();
        return tenantLogRepository.findLastLogDate(tenantIds, logType)
                .or(() -> fallbacks.filter(Objects::nonNull).max(Comparator.naturalOrder()))
                .orElseGet(() -> {
                    log.warn("No {} date found for apartment sharing {}, using today", logType, apartmentSharing.getId());
                    return LocalDateTime.now();
                })
                .toLocalDate();
    }

    private static byte[] qrCode() {
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(new QRCodeWriter().encode(QUESTIONNAIRE_URL, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE), "PNG", png);
            return png.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Unable to generate the questionnaire QR code", e);
        }
    }
}
