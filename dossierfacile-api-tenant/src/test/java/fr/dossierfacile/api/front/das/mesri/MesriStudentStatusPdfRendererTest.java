package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MesriStudentStatusPdfRendererTest {

    private final MesriStudentStatusPdfRenderer renderer = new MesriStudentStatusPdfRenderer(new ObjectMapper());

    private static MesriStudentStatus sandboxStatus(String rawJson) {
        return new MesriStudentStatus(rawJson, "https://staging.particulier.api.gouv.fr/v3/mesri/statut_etudiant/france_connect",
                "12006701200027", List.of(new MesriStudentStatus.Admission("2020-07-01", "2021-08-31", true, "RF1",
                        "formation initiale", "33063", "0330023W", "LYCEE GENERAL ET TECHNOLOGIQUE CAMILLE JULLIAN")));
    }

    @Test
    void rendersHeaderAndFullJson() throws Exception {
        byte[] pdf = renderer.render(sandboxStatus(ApiParticulierClientImplTest.SANDBOX_BODY), "Angela DUBOIS",
                LocalDateTime.of(2026, 10, 16, 15, 4));

        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(text).contains("Statut étudiant")
                    .contains("Récupéré le 16/10/2026 à 15:04")
                    .contains("Locataire : Angela DUBOIS")
                    .contains("12006701200027")
                    .contains("\"uai\" : \"0330023W\"")
                    .contains("CAMILLE JULLIAN");
        }
    }

    @Test
    void longJsonBreaksIntoSeveralPages() throws Exception {
        StringBuilder big = new StringBuilder("{\"data\":{\"admissions\":[");
        for (int i = 0; i < 80; i++) {
            big.append(i > 0 ? "," : "").append("{\"date_debut\":\"2020-07-01\",\"date_fin\":\"2021-08-31\",\"est_inscrit\":true,"
                    + "\"etablissement_etudes\":{\"uai\":\"000000").append(i).append("\",\"nom\":\"ETABLISSEMENT ").append(i).append("\"}}");
        }
        big.append("]}}");

        byte[] pdf = renderer.render(sandboxStatus(big.toString()), "Jean MESRI", LocalDateTime.now());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(document)).contains("ETABLISSEMENT 79");
        }
    }

    @Test
    void unencodableCharactersAreReplacedNotFatal() throws Exception {
        String raw = "{\"nom\":\"Établissement € 日本 ✓\"}";

        byte[] pdf = renderer.render(sandboxStatus(raw), "Zoë N'Guyen", LocalDateTime.now());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Établissement €").contains("Zoë N'Guyen").doesNotContain("日本");
        }
    }

    @Test
    void wrapKeepsIndentationOnContinuationLines() {
        List<String> lines = MesriStudentStatusPdfRenderer.wrap("    \"k\" : \"" + "x".repeat(100) + "\"", 40);

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).hasSize(40);
        assertThat(lines.get(1)).startsWith("    x");
    }

    @Test
    void sanitizeReplacesOnlyUnsupportedCharacters() {
        String out = MesriStudentStatusPdfRenderer.sanitize(new PDType1Font(Standard14Fonts.FontName.COURIER), "é€→");

        assertThat(out).isEqualTo("é€?");
    }
}
