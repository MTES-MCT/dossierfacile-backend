package fr.dossierfacile.api.front.das.mesri;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * "Relevé de statut étudiant": the student statement rendered from the MESR data, following the DossierFacile
 * mock-up (header with the RF / DossierFacile logos, holder, declared period, one card per admission, footer).
 * Pure PDFBox drawing, A4, Marianne fonts shipped in {@code static/fonts}.
 */
@Slf4j
@Component
public class MesriStudentStatusPdfRenderer {

    static final String TITLE = "Relevé de statut étudiant";
    static final String NOTE = "Toutes les informations de ce relevé correspondent aux renseignements transmis par le ministère "
            + "de l'Enseignement supérieur et de la Recherche. Ces informations sont l'équivalent d'un certificat étudiant.";

    /** RF block + DossierFacile wordmark, rasterised at 300 dpi from the pdf-generator header template. */
    private static final String HEADER_LOGO = "static/img/header-rf-dossierfacile.png";
    private static final float HEADER_LOGO_W = 271f;
    private static final String FONT_REGULAR = "static/fonts/Marianne-Regular.ttf";
    private static final String FONT_BOLD = "static/fonts/Marianne-Bold.ttf";

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN_X = 56f;
    private static final float MARGIN_TOP = 48f;
    private static final float MARGIN_BOTTOM = 48f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN_X;
    private static final float HEADER_BAND_H = 110f;
    private static final float FOOTER_H = 66f;

    private static final float BODY = 10f;
    private static final float BODY_LEADING = 15f;
    private static final float SMALL = 8.5f;
    private static final float SMALL_LEADING = 12f;
    private static final float PAD = 12f;

    private static final Color INK = new Color(0x16, 0x16, 0x16);
    private static final Color INK_SOFT = new Color(0x3A, 0x3A, 0x3A);
    private static final Color GREY = new Color(0x66, 0x66, 0x66);
    private static final Color BORDER = new Color(0xDD, 0xDD, 0xDD);
    private static final Color CARD_HEAD = new Color(0xF6, 0xF6, 0xF6);
    private static final Color BAND = new Color(0xF5, 0xF5, 0xFE);
    private static final Color BLUE = new Color(0x00, 0x00, 0x91);
    private static final Color SUCCESS_BG = new Color(0xB8, 0xFE, 0xC9);
    private static final Color SUCCESS_FG = new Color(0x18, 0x75, 0x3C);
    private static final Color NEUTRAL_BG = new Color(0xEE, 0xEE, 0xEE);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] render(MesriStudentStatus status, StudentStatementHolder holder, ZonedDateTime retrievedAt) throws IOException {
        List<MesriStudentStatus.Admission> admissions = new ArrayList<>(status.admissions());
        admissions.sort(Comparator.comparing((MesriStudentStatus.Admission a) -> nullSafe(a.dateDebut())).reversed());

        try (PDDocument document = new PDDocument()) {
            document.getDocumentInformation().setTitle(TITLE);
            document.getDocumentInformation().setCreator("DossierFacile");
            Fonts fonts = Fonts.load(document);
            PDImageXObject logo = loadHeaderLogo(document);

            Writer w = new Writer(document, fonts, logo, retrievedAt);
            w.newPage();

            // Title and holder
            w.y -= 6f;
            w.text(TITLE, fonts.bold, 20f, INK, MARGIN_X, w.y);
            w.y -= 30f;
            w.labelValue("Délivré à : ", holder.fullName(), MARGIN_X);
            w.y -= BODY_LEADING;
            w.labelValue("Date de naissance : ", holder.birthDate() == null ? "non transmise" : holder.birthDate().format(DAY), MARGIN_X);
            w.y -= BODY_LEADING + 6f;
            for (String line : w.wrap(NOTE, fonts.regular, SMALL + 0.5f, CONTENT_W)) {
                w.text(line, fonts.regular, SMALL + 0.5f, INK_SOFT, MARGIN_X, w.y);
                w.y -= SMALL_LEADING + 1f;
            }
            w.y -= 10f;

            // Declared period band
            String from = monthYear(
                    admissions.stream().map(MesriStudentStatus.Admission::dateDebut).filter(d -> d != null).min(String::compareTo).orElse(null));
            String to = monthYear(admissions.stream().map(MesriStudentStatus.Admission::dateFin).filter(d -> d != null).max(String::compareTo).orElse(null));
            float bandH = 40f;
            w.ensure(bandH);
            w.rect(MARGIN_X, w.y - bandH, CONTENT_W, bandH, BAND, null);
            w.text("Inscriptions déclarées", fonts.bold, BODY, INK, MARGIN_X + PAD, w.y - 24f);
            float toW = Math.max(w.width(to, fonts.bold, BODY), 40f);
            float toX = MARGIN_X + CONTENT_W - PAD - toW;
            float fromX = toX - 24f - Math.max(w.width(from, fonts.bold, BODY), 40f);
            w.text("de", fonts.regular, SMALL, GREY, fromX, w.y - 14f);
            w.text(from, fonts.bold, BODY, INK, fromX, w.y - 29f);
            w.text("à", fonts.regular, SMALL, GREY, toX, w.y - 14f);
            w.text(to, fonts.bold, BODY, INK, toX, w.y - 29f);
            w.y -= bandH + 14f;

            // One card per admission, most recent first, never split across pages
            for (MesriStudentStatus.Admission admission : admissions) {
                w.card(admission);
                w.y -= 12f;
            }

            w.finish(status, holder);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String day(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DAY);
        } catch (RuntimeException e) {
            return isoDate == null ? "?" : isoDate;
        }
    }

    static String monthYear(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(MONTH_YEAR);
        } catch (RuntimeException e) {
            return isoDate == null ? "?" : isoDate;
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static PDImageXObject loadHeaderLogo(PDDocument document) throws IOException {
        try (InputStream is = MesriStudentStatusPdfRenderer.class.getClassLoader().getResourceAsStream(HEADER_LOGO)) {
            if (is == null) {
                log.warn("Header logo {} not found, the statement will have no logo", HEADER_LOGO);
                return null;
            }
            return PDImageXObject.createFromByteArray(document, is.readAllBytes(), "header-logo");
        }
    }

    private record Fonts(PDFont regular, PDFont bold) {
        static Fonts load(PDDocument document) throws IOException {
            return new Fonts(loadFont(document, FONT_REGULAR), loadFont(document, FONT_BOLD));
        }

        private static PDFont loadFont(PDDocument document, String path) throws IOException {
            try (InputStream is = MesriStudentStatusPdfRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IOException("Font not found on classpath: " + path);
                }
                return PDType0Font.load(document, is);
            }
        }
    }

    /** Top-down writer with page breaks; footers are drawn once the page count is known. */
    private final class Writer {
        private final PDDocument document;
        private final Fonts fonts;
        private final PDImageXObject logo;
        private final ZonedDateTime retrievedAt;
        private final List<PDPage> pages = new ArrayList<>();
        private PDPageContentStream cs;
        private float y;

        Writer(PDDocument document, Fonts fonts, PDImageXObject logo, ZonedDateTime retrievedAt) {
            this.document = document;
            this.fonts = fonts;
            this.logo = logo;
            this.retrievedAt = retrievedAt;
        }

        void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            pages.add(page);
            cs = new PDPageContentStream(document, page);
            if (logo != null) {
                // The raster keeps a white margin around the logos, hence the slight overshoot above the top margin
                float h = HEADER_LOGO_W * logo.getHeight() / logo.getWidth();
                cs.drawImage(logo, MARGIN_X - 10f, PAGE_H - MARGIN_TOP + 12f - h, HEADER_LOGO_W, h);
            }
            // Retrieval date, right aligned on the band
            String dateLine = retrievedAt.format(LONG_DAY);
            String timeLine = "à " + retrievedAt.format(TIME) + " heure de Paris";
            text(dateLine, fonts.bold, BODY, INK, PAGE_W - MARGIN_X - width(dateLine, fonts.bold, BODY), PAGE_H - MARGIN_TOP - 10f);
            text(timeLine, fonts.regular, SMALL, GREY, PAGE_W - MARGIN_X - width(timeLine, fonts.regular, SMALL), PAGE_H - MARGIN_TOP - 24f);
            y = PAGE_H - HEADER_BAND_H - 34f;
        }

        void ensure(float height) throws IOException {
            if (y - height < MARGIN_BOTTOM + FOOTER_H) {
                newPage();
            }
        }

        void card(MesriStudentStatus.Admission a) throws IOException {
            String name = a.etablissementNom() == null ? "Établissement non renseigné" : a.etablissementNom();
            List<String> nameLines = wrap(name, fonts.bold, BODY, CONTENT_W - 2 * PAD - width("Nom : ", fonts.regular, BODY));
            float headH = PAD + 11f + 3f + nameLines.size() * BODY_LEADING + 2 * BODY_LEADING + PAD - 2f;
            float bodyH = PAD + BODY_LEADING + 10f + 3 * BODY_LEADING + PAD - 2f;
            float cardH = headH + bodyH;
            ensure(cardH);

            float top = y;
            // Frame and card head background
            rect(MARGIN_X, top - cardH, CONTENT_W, cardH, null, BORDER);
            rect(MARGIN_X, top - headH, CONTENT_W, headH, CARD_HEAD, null);
            line(MARGIN_X, top - headH, MARGIN_X + CONTENT_W, top - headH, BORDER);

            float x = MARGIN_X + PAD;
            float cy = top - PAD - 9f;
            text("ÉTABLISSEMENT", fonts.bold, 8f, BLUE, x, cy);
            cy -= BODY_LEADING;
            float nameX = x + width("Nom : ", fonts.regular, BODY);
            text("Nom : ", fonts.regular, BODY, INK, x, cy);
            for (String l : nameLines) {
                text(l, fonts.bold, BODY, INK, nameX, cy);
                cy -= BODY_LEADING;
            }
            text("Code UAI : " + nullOr(a.uai()), fonts.regular, BODY, INK, x, cy);
            cy -= BODY_LEADING;
            text("Commune (code INSEE) : " + nullOr(a.communeCode()), fonts.regular, BODY, INK, x, cy);

            cy = top - headH - PAD - 9f;
            String period = "Du " + day(a.dateDebut()) + " au " + day(a.dateFin());
            text(period, fonts.bold, BODY, INK, x, cy);
            badge(Boolean.TRUE.equals(a.estInscrit()), x + width(period, fonts.bold, BODY) + 8f, cy);
            cy -= BODY_LEADING + 10f;
            text("Inscription", fonts.bold, BODY, INK, x, cy);
            cy -= BODY_LEADING;
            text("Régime de formation : " + capitalize(a.regimeLibelle()), fonts.regular, BODY, INK, x, cy);
            cy -= BODY_LEADING;
            text("Code régime : " + nullOr(a.regimeCode()), fonts.regular, BODY, INK, x, cy);

            y = top - cardH;
        }

        private void badge(boolean inscrit, float x, float baseline) throws IOException {
            String label = inscrit ? "INSCRIT" : "NON INSCRIT";
            float w = width(label, fonts.bold, 8f) + 12f;
            rect(x, baseline - 4f, w, 15f, inscrit ? SUCCESS_BG : NEUTRAL_BG, null);
            text(label, fonts.bold, 8f, inscrit ? SUCCESS_FG : GREY, x + 6f, baseline);
        }

        void finish(MesriStudentStatus status, StudentStatementHolder holder) throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
            int total = pages.size();
            String source = "Source : API Particulier – Statut étudiant (ministère de l'Enseignement supérieur et de la Recherche)";
            String retrieved = "Récupéré le " + retrievedAt.format(DAY) + " à " + retrievedAt.format(TIME) + " · Locataire : " + holder.fullName()
                    + " (identité FranceConnect)";
            String siret = "SIRET destinataire : " + status.recipientSiret();
            for (int i = 0; i < total; i++) {
                try (PDPageContentStream fs = new PDPageContentStream(document, pages.get(i), AppendMode.APPEND, true, true)) {
                    cs = fs;
                    float top = MARGIN_BOTTOM + FOOTER_H;
                    line(MARGIN_X, top, MARGIN_X + CONTENT_W, top, BORDER);
                    float fy = top - 2f;
                    for (String paragraph : List.of(source, retrieved, siret)) {
                        for (String l : wrap(paragraph, fonts.regular, SMALL, CONTENT_W)) {
                            fy -= SMALL_LEADING;
                            text(l, fonts.regular, SMALL, GREY, MARGIN_X, fy);
                        }
                    }
                    String pageLabel = (i + 1) + " / " + total;
                    String site = "www.dossierfacile.logement.gouv.fr";
                    text(pageLabel, fonts.regular, SMALL, GREY, MARGIN_X, MARGIN_BOTTOM + 4f);
                    text(site, fonts.regular, SMALL, GREY, MARGIN_X + CONTENT_W - width(site, fonts.regular, SMALL), MARGIN_BOTTOM + 4f);
                } finally {
                    cs = null;
                }
            }
        }

        void labelValue(String label, String value, float x) throws IOException {
            text(label, fonts.regular, BODY, INK, x, y);
            text(value, fonts.bold, BODY, INK, x + width(label, fonts.regular, BODY), y);
        }

        void text(String text, PDFont font, float size, Color color, float x, float baseline) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, baseline);
            cs.showText(sanitize(font, text));
            cs.endText();
        }

        void rect(float x, float y0, float w, float h, Color fill, Color stroke) throws IOException {
            if (fill != null) {
                cs.setNonStrokingColor(fill);
                cs.addRect(x, y0, w, h);
                cs.fill();
            }
            if (stroke != null) {
                cs.setStrokingColor(stroke);
                cs.setLineWidth(0.75f);
                cs.addRect(x, y0, w, h);
                cs.stroke();
            }
        }

        void line(float x1, float y1, float x2, float y2, Color color) throws IOException {
            cs.setStrokingColor(color);
            cs.setLineWidth(0.75f);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        float width(String text, PDFont font, float size) throws IOException {
            return font.getStringWidth(sanitize(font, text)) / 1000f * size;
        }

        List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(candidate, font, size) > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private String nullOr(String s) {
            return s == null ? "non renseigné" : s;
        }
    }

    /** Characters missing from the font are replaced so a rare glyph never fails the whole document. */
    static String sanitize(PDFont font, String text) {
        if (text == null) {
            return "";
        }
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
}
