package fr.dossierfacile.api.front.das.mesri;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MesriStudentStatusPdfRendererTest {

    private static final ZonedDateTime RETRIEVED_AT = ZonedDateTime.of(2026, 10, 20, 14, 32, 0, 0, ZoneId.of("Europe/Paris"));
    private static final StudentStatementHolder ANGELA = new StudentStatementHolder("Angela Claire Louise DUBOIS", LocalDate.of(1962, 8, 24));

    private final MesriStudentStatusPdfRenderer renderer = new MesriStudentStatusPdfRenderer();

    private static MesriStudentStatus.Admission admission(String debut, String fin, boolean inscrit, String regime, String libelle, String uai, String nom) {
        return new MesriStudentStatus.Admission(debut, fin, inscrit, regime, libelle, "33063", uai, nom);
    }

    /** The sandbox FranceConnect case of Angela DUBOIS: one admission, no identity block. */
    private static MesriStudentStatus angelaSandbox() {
        return new MesriStudentStatus(ApiParticulierClientImplTest.SANDBOX_BODY,
                "https://staging.particulier.api.gouv.fr/v3/mesri/statut_etudiant/france_connect", "12006701200027", null,
                List.of(admission("2020-07-01", "2021-08-31", true, "RF1", "formation initiale", "0330023W",
                        "LYCEE GENERAL ET TECHNOLOGIQUE CAMILLE JULLIAN")));
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void rendersTheStatementForTheSandboxCase() throws Exception {
        byte[] pdf = renderer.render(angelaSandbox(), ANGELA, RETRIEVED_AT);
        // Kept for a visual check: target/mesri-statement-sample.pdf
        Files.write(new File("target/mesri-statement-sample.pdf").toPath(), pdf);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
        String text = text(pdf);
        assertThat(text)
                .contains("Relevé de statut étudiant")
                .contains("20 octobre 2026")
                .contains("14:32 heure de Paris")
                .contains("Délivré à : Angela Claire Louise DUBOIS")
                .contains("Date de naissance : 24/08/1962")
                .contains("équivalent d'un certificat étudiant")
                .contains("Inscriptions déclarées")
                .contains("juillet 2020")
                .contains("août 2021")
                .contains("ÉTABLISSEMENT")
                .contains("Nom : LYCEE GENERAL ET TECHNOLOGIQUE CAMILLE JULLIAN")
                .contains("Code UAI : 0330023W")
                .contains("Commune (code INSEE) : 33063")
                .contains("Du 01/07/2020 au 31/08/2021")
                .contains("INSCRIT")
                .contains("Régime de formation : Formation initiale")
                .contains("Code régime : RF1")
                .contains("SIRET destinataire")
                .contains("12006701200027")
                .contains("1 / 1")
                .doesNotContain("\"admissions\"")
                .doesNotContain("Attestation sur l");
    }

    @Test
    void sortsAdmissionsMostRecentFirstAndSpansTheWholePeriod() throws Exception {
        MesriStudentStatus status = new MesriStudentStatus("{}", "url", "siret", null, List.of(
                admission("2021-09-01", "2022-08-31", false, "RF1", "formation initiale", "0010013J", "LYCEE LALANDE"),
                admission("2022-10-01", "2023-06-30", true, "RF2", "formation continue", "0010010F", "UNIVERSITE LYON 1")));

        String text = text(renderer.render(status, new StudentStatementHolder("Georges MOUSTAKI", null), RETRIEVED_AT));

        assertThat(text).contains("septembre 2021").contains("juin 2023")
                .contains("Date de naissance : non transmise")
                .contains("NON INSCRIT")
                .contains("Régime de formation : Formation continue");
        assertThat(text.indexOf("UNIVERSITE LYON 1")).isLessThan(text.indexOf("LYCEE LALANDE"));
    }

    @Test
    void manyAdmissionsFlowOverSeveralNumberedPages() throws Exception {
        List<MesriStudentStatus.Admission> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(admission("20" + (10 + i) + "-09-01", "20" + (11 + i) + "-08-31", i % 2 == 0, "RF1", "formation initiale", "000000" + i,
                    "ETABLISSEMENT " + i + " AVEC UN NOM PARTICULIEREMENT LONG POUR FORCER LE RETOUR A LA LIGNE DANS LA CARTE"));
        }
        byte[] pdf = renderer.render(new MesriStudentStatus("{}", "url", "siret", null, many), ANGELA, RETRIEVED_AT);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("1 / " + document.getNumberOfPages()).contains("ETABLISSEMENT 0").contains("ETABLISSEMENT 11");
        }
    }

    @Test
    void rareGlyphsAreReplacedNotFatal() throws Exception {
        MesriStudentStatus status = new MesriStudentStatus("{}", "url", "siret", null,
                List.of(admission("2020-07-01", "2021-08-31", true, "RF1", "formation initiale", "0330023W", "Établissement 日本 ✓")));

        String text = text(renderer.render(status, new StudentStatementHolder("Zoë N'Guyen", null), RETRIEVED_AT));

        assertThat(text).contains("Établissement").contains("Zoë N'Guyen").doesNotContain("日本");
    }

    @Test
    void helpersFormatFrenchDates() {
        assertThat(MesriStudentStatusPdfRenderer.day("2021-08-31")).isEqualTo("31/08/2021");
        assertThat(MesriStudentStatusPdfRenderer.monthYear("2021-08-31")).isEqualTo("août 2021");
        assertThat(MesriStudentStatusPdfRenderer.day(null)).isEqualTo("?");
        assertThat(MesriStudentStatusPdfRenderer.capitalize("formation continue")).isEqualTo("Formation continue");
    }
}
