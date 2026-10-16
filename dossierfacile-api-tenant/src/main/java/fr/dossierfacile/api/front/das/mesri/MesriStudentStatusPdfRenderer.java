package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * First iteration of the student status document: a header, then the full JSON returned by API Particulier,
 * pretty-printed in a monospaced font. No business formatting yet, operators read the raw data.
 */
@Component
@RequiredArgsConstructor
public class MesriStudentStatusPdfRenderer {

    static final String TITLE = "Statut étudiant – données MESR obtenues via FranceConnect";
    private static final float MARGIN = 50f;
    private static final float TITLE_SIZE = 14f;
    private static final float HEADER_SIZE = 10f;
    private static final float BODY_SIZE = 9f;
    private static final float HEADER_LEADING = 14f;
    private static final float BODY_LEADING = 11f;
    /** Courier 9 pt is 5.4 pt wide: 90 characters fit in the 495 pt text column of an A4 page. */
    private static final int BODY_MAX_CHARS = 90;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final ObjectMapper objectMapper;

    public byte[] render(MesriStudentStatus status, String tenantFullName, LocalDateTime retrievedAt) throws IOException {
        PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.COURIER);

        List<String> headerLines = List.of(
                "Récupéré le " + retrievedAt.format(DATE_FORMAT),
                "Locataire : " + tenantFullName,
                "Destinataire (SIRET) : " + status.recipientSiret(),
                "Source : " + status.sourceUrl(),
                "Nombre d'admissions : " + status.admissions().size(),
                "",
                "Réponse intégrale de l'API :");
        List<String> bodyLines = wrap(prettyPrint(status.rawJson()), BODY_MAX_CHARS);

        try (PDDocument document = new PDDocument()) {
            document.getDocumentInformation().setTitle(TITLE);
            document.getDocumentInformation().setCreator("DossierFacile");

            PageWriter writer = new PageWriter(document);
            writer.newPage();
            writer.write(titleFont, TITLE_SIZE, 18f, sanitize(titleFont, TITLE));
            writer.y -= 6f;
            for (String line : headerLines) {
                writer.write(headerFont, HEADER_SIZE, HEADER_LEADING, sanitize(headerFont, line));
            }
            writer.y -= 6f;
            for (String line : bodyLines) {
                writer.write(bodyFont, BODY_SIZE, BODY_LEADING, sanitize(bodyFont, line));
            }
            writer.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private String prettyPrint(String rawJson) {
        try {
            Object tree = objectMapper.readTree(rawJson);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (IOException e) {
            return rawJson;
        }
    }

    /** Hard-wraps lines longer than {@code maxChars}, keeping the original indentation on continuation lines. */
    static List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            int indent = line.length() - line.stripLeading().length();
            String prefix = " ".repeat(Math.min(indent, maxChars / 2));
            String rest = line;
            while (rest.length() > maxChars) {
                lines.add(rest.substring(0, maxChars));
                rest = prefix + rest.substring(maxChars);
            }
            lines.add(rest);
        }
        return lines;
    }

    /** Standard 14 fonts only cover WinAnsi: any other character is replaced to avoid a render failure. */
    static String sanitize(PDFont font, String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            try {
                font.encode(ch);
                sb.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /** Minimal top-down text writer with automatic page breaks. */
    private static final class PageWriter {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        PageWriter(PDDocument document) {
            this.document = document;
        }

        void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        void write(PDFont font, float size, float leading, String text) throws IOException {
            if (y - leading < MARGIN) {
                newPage();
            }
            y -= leading;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
        }

        void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
