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
import fr.dossierfacile.common.enums.TenantFileStatus;
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
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        LocalDate requestDate = status == DossierPdfStatus.TO_PROCESS ? requestDate(apartmentSharing) : null;
        DossierPdfModel model = new DossierPdfModelBuilder(resources, this::pieceContents).build(apartmentSharing, status, date, requestDate);
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
            pdfSignatureService.signAndSaveKeepingMetadata(document, signed);
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
        List<Tenant> tenants = apartmentSharing.getTenants();
        return lastLogDate(tenants, LogType.ACCOUNT_VALIDATED)
                .or(() -> lastDate(tenants.stream().map(Tenant::getOperatorDateTime)))
                .orElseGet(() -> today(apartmentSharing, LogType.ACCOUNT_VALIDATED));
    }

    /**
     * Returns the date of the last submission among the tenants, or else the date of the last document.
     */
    private LocalDate depositDate(ApartmentSharing apartmentSharing) {
        List<Tenant> tenants = apartmentSharing.getTenants();
        return lastLogDate(tenants, LogType.ACCOUNT_COMPLETED)
                .or(() -> lastDate(tenants.stream()
                        .flatMap(tenant -> Stream.concat(tenant.getDocuments().stream(),
                                tenant.getGuarantors().stream().flatMap(g -> g.getDocuments().stream())))
                        .map(Document::getCreationDateTime)))
                .orElseGet(() -> today(apartmentSharing, LogType.ACCOUNT_COMPLETED));
    }

    /**
     * Returns the date when the last tenant that waits for a verification entered the queue, or else its submission
     * date. The older dossiers have no log of their entry.
     */
    private LocalDate requestDate(ApartmentSharing apartmentSharing) {
        List<Tenant> tenants = apartmentSharing.getTenants().stream().filter(t -> t.getStatus() == TenantFileStatus.TO_PROCESS).toList();
        return lastLogDate(tenants, LogType.QUEUE_ENTERED)
                .or(() -> lastLogDate(tenants, LogType.ACCOUNT_COMPLETED))
                .orElseGet(() -> depositDate(apartmentSharing));
    }

    private Optional<LocalDate> lastLogDate(List<Tenant> tenants, LogType logType) {
        List<Long> tenantIds = tenants.stream().map(Tenant::getId).filter(Objects::nonNull).toList();
        return tenantLogRepository.findLastLogDate(tenantIds, logType).map(LocalDateTime::toLocalDate);
    }

    private static Optional<LocalDate> lastDate(Stream<LocalDateTime> dates) {
        return dates.filter(Objects::nonNull).max(Comparator.naturalOrder()).map(LocalDateTime::toLocalDate);
    }

    private static LocalDate today(ApartmentSharing apartmentSharing, LogType logType) {
        log.warn("No {} date found for apartment sharing {}, using today", logType, apartmentSharing.getId());
        return LocalDate.now(ZoneId.systemDefault());
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
