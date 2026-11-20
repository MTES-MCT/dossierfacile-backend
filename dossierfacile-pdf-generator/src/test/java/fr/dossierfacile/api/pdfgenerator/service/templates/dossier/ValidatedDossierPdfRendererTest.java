package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.PdfSignatureServiceImpl;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.TenantLogRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Generates the validated dossier PDF for a single candidate, a couple and a flatsharing of seven,
 * all with very long names. Open target/dossier-pdf/*.pdf to look at the result.
 */
class ValidatedDossierPdfRendererTest {

    private static final File OUTPUT = new File("target/dossier-pdf");
    private static final Pattern SUMMARY_LINE = Pattern.compile("(?m)^ *p\\. \\d+ ");

    private ValidatedDossierPdfRenderer renderer;

    @BeforeEach
    void setUp() {
        DownloadService downloadService = mock(DownloadService.class);
        when(downloadService.getDocumentInputStream(any())).then(invocation -> piece(invocation.getArgument(0)));
        TenantLogRepository tenantLogRepository = mock(TenantLogRepository.class);
        when(tenantLogRepository.findLastLogDate(anyCollection(), eq(LogType.ACCOUNT_VALIDATED)))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 10, 6, 14, 30)));
        renderer = new ValidatedDossierPdfRenderer(downloadService, tenantLogRepository, new PdfSignatureServiceImpl(), new PieceExtractor());
        OUTPUT.mkdirs();
    }

    @Test
    void should_render_dossier_of_a_single_candidate() throws IOException {
        String text = renderAndCheck(DossierFixtures.alone(), "validated-alone.pdf",
                "Dossier de location d'Anne-Laure Bourdonnaye-Lanvaux De Kerangal-Le Bihan");

        assertThat(text)
                .contains("1 candidat")
                .contains("3\u00A0150\u00A0€ cumulés")
                .contains("Deux personnes")
                .contains("Le mot du candidat")
                .contains("Le justificatif de ressources n°\u00A03 d'Anne-Laure Bourdonnaye-Lanvaux De Kerangal-Le Bihan");
    }

    @Test
    void should_render_dossier_of_a_couple() throws IOException {
        String text = renderAndCheck(DossierFixtures.couple(), "validated-couple.pdf",
                "Dossier de location de Marie-Clémence Vandenberghe-Quéméneur De La Villemarqué et Jean-Baptiste Van Der Straeten-Oudinot De Rosnay");

        assertThat(text)
                .contains("2 candidats")
                .contains("Un organisme")
                .contains("Le certificat de l'organisme garant de Jean-Baptiste Van Der Straeten-Oudinot De Rosnay");
    }

    @Test
    void should_render_dossier_of_a_large_flatsharing() throws IOException {
        String text = renderAndCheck(DossierFixtures.largeFlatsharing(), "validated-flatsharing.pdf",
                "Dossier de location de Marie-Clémence Vandenberghe-Quéméneur De La Villemarqué, Jean-Baptiste Van Der Straeten-Oudinot De Rosnay,");

        assertThat(text)
                .contains("7 candidats")
                .contains("Sommaire, suite")
                .contains("Une personne morale")
                .contains("Deux organismes");
    }

    @Test
    void should_render_dossier_with_short_names() throws IOException {
        String text = renderAndCheck(DossierFixtures.shortNames(), "validated-short-names.pdf",
                "Dossier de location de Camille Lefèvre, Nadia Colin et Thomas Orio");

        assertThat(text).contains("Dossier de location de\nCamille Lefèvre, Nadia Colin et Thomas Orio").doesNotContain("Garant de Camille, suite");
    }

    @Test
    void should_keep_the_text_of_a_sworn_statement() throws IOException {
        ApartmentSharing apartmentSharing = DossierFixtures.shortNames();
        documents(apartmentSharing).stream()
                .filter(d -> d.getDocumentCategory() == DocumentCategory.TAX)
                .findFirst().orElseThrow()
                .setDocumentSubCategory(DocumentSubCategory.OTHER_TAX);

        String text = renderAndCheck(apartmentSharing, "validated-statement.pdf", "Dossier de location de Camille Lefèvre");

        assertThat(text).contains("Je ne suis pas en mesure de fournir un avis d'imposition.").contains("Deuxième page.");
        try (PDDocument document = Loader.loadPDF(new File(OUTPUT, "validated-statement.pdf"))) {
            assertThat(alternatives(document.getDocumentCatalog().getStructureTreeRoot().getKids()))
                    .contains("L'avis d'imposition de Camille Lefèvre, fichier déposé, page 1 sur 2. Contenu : "
                            + "Je ne suis pas en mesure de fournir un avis d'imposition.",
                            "L'avis d'imposition de Camille Lefèvre, fichier déposé, page 2 sur 2. Contenu : Deuxième page.",
                            "Le justificatif d'hébergement de Camille Lefèvre, fichier déposé");
        }
    }

    @Test
    void should_ignore_the_characters_that_xml_forbids() throws IOException {
        ApartmentSharing apartmentSharing = DossierFixtures.shortNames();
        apartmentSharing.getTenants().get(0).setClarification("Bonjour\u000B\u0003 & <b>merci</b> \uD83D fin \uFFFE 🙂 ]]>");

        String text = renderAndCheck(apartmentSharing, "validated-invalid-characters.pdf", "Dossier de location de Camille Lefèvre");

        assertThat(text).contains("Bonjour & <b>merci</b>").contains("fin");
    }

    @Test
    void should_render_dossier_with_missing_names() throws IOException {
        ApartmentSharing apartmentSharing = DossierFixtures.shortNames();
        apartmentSharing.getTenants().get(0).setFirstName(null);
        apartmentSharing.getTenants().get(1).setLastName(null);
        apartmentSharing.getTenants().get(2).setEmail(null);

        String text = renderAndCheck(apartmentSharing, "validated-missing-names.pdf", "Dossier de location de Lefèvre, Nadia et Thomas Orio");

        assertThat(text).contains("La pièce d'identité de Lefèvre").contains("La pièce d'identité de Nadia");
    }

    @Test
    void should_announce_the_person_that_continues_in_the_next_column() throws IOException {
        ApartmentSharing apartmentSharing = DossierFixtures.shortNames();
        // The long name leaves no room for the last pieces of Odile.
        apartmentSharing.getTenants().get(0).getGuarantors().get(0).setFirstName("Frédéric ".repeat(18).strip());

        String text = renderAndCheck(apartmentSharing, "validated-continued.pdf", "Dossier de location de Camille Lefèvre");

        assertThat(text).contains("Garant de Camille, suite\nOdile Lefèvre");
    }

    @Test
    void should_leave_room_for_a_guarantor_line_that_wraps() throws IOException {
        String text = renderAndCheck(DossierFixtures.fourGuarantors(), "validated-four-guarantors.pdf", "Dossier de location de Camille Lefèvre");

        assertThat(text).contains("Quatre personnes\n(18\u00A0000\u00A0€ cumulés)");
    }

    /** Looks for a block that the layout measures badly. Such a block creates an extra page, or hides a piece of the summary. */
    @Test
    void should_never_overflow() throws IOException {
        for (int seed = 0; seed < 10; seed++) {
            ApartmentSharing apartmentSharing = DossierFixtures.random(seed);

            String text = renderAndCheck(apartmentSharing, "validated-random.pdf", "Dossier de location");

            assertThat(SUMMARY_LINE.matcher(text).results()).as("summary lines, seed %d", seed).hasSize(numberOfSummaryLines(apartmentSharing));
        }
    }

    private String renderAndCheck(ApartmentSharing apartmentSharing, String fileName, String titleStart) throws IOException {
        byte[] pdf;
        try (InputStream rendered = renderer.render(apartmentSharing)) {
            pdf = rendered.readAllBytes();
        }
        Files.write(new File(OUTPUT, fileName).toPath(), pdf);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getDocumentInformation().getTitle()).startsWith(titleStart);
            assertThat(document.getDocumentCatalog().getLanguage()).isEqualTo("fr");
            assertThat(document.getDocumentCatalog().getMarkInfo().isMarked()).isTrue();
            assertThat(document.getDocumentCatalog().getDocumentOutline()).isNotNull();
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // Nothing overflows when each planned page is one page of the PDF.
            int summaryPages = 1 + StringUtils.countMatches(text, "Sommaire, suite");
            int pieces = StringUtils.countMatches(text, "PIÈCE VÉRIFIÉE");
            int clarificationPages = apartmentSharing.getTenants().get(0).getClarification() == null ? 0 : 1;
            assertThat(document.getNumberOfPages()).as("pages of %s", titleStart).isEqualTo(summaryPages + clarificationPages + pieces + 1);
            assertThat(pieces).isEqualTo(numberOfPiecePages(apartmentSharing));
            assertThat(text).contains("DOSSIER VALIDÉ").contains("6 octobre 2026").contains("Comprendre les statuts");
            return text;
        }
    }

    private static int numberOfPiecePages(ApartmentSharing apartmentSharing) throws IOException {
        int pages = 0;
        for (Document document : documents(apartmentSharing)) {
            try (PDDocument piece = Loader.loadPDF(piece(document).readAllBytes())) {
                pages += piece.getNumberOfPages();
            }
        }
        return pages;
    }

    private static int numberOfSummaryLines(ApartmentSharing apartmentSharing) {
        return apartmentSharing.getTenants().stream()
                .flatMap(t -> Stream.concat(Stream.of(t.getDocuments()), t.getGuarantors().stream().map(Guarantor::getDocuments)))
                .mapToInt(documents -> (int) documents.stream().map(Document::getDocumentCategory).distinct().count())
                .sum();
    }

    private static List<Document> documents(ApartmentSharing apartmentSharing) {
        return apartmentSharing.getTenants().stream()
                .flatMap(t -> Stream.concat(t.getDocuments().stream(), t.getGuarantors().stream().flatMap(g -> g.getDocuments().stream())))
                .toList();
    }

    private static List<String> alternatives(List<Object> structure) {
        List<String> alternatives = new ArrayList<>();
        for (Object kid : structure) {
            if (kid instanceof PDStructureElement element) {
                if ("Figure".equals(element.getStructureType()) && element.getAlternateDescription() != null) {
                    alternatives.add(element.getAlternateDescription());
                }
                alternatives.addAll(alternatives(element.getKids()));
            }
        }
        return alternatives;
    }

    /** The pieces are test files: a scan saved as an image, a JPEG page, a landscape page and a sworn statement of two pages. */
    private static InputStream piece(Document document) {
        if (document.getDocumentSubCategory() == DocumentSubCategory.OTHER_TAX) {
            return new ByteArrayInputStream(TestPdfs.statement("Je ne suis pas en mesure de fournir un avis d'imposition.", "Deuxième page."));
        }
        String file = switch (document.getDocumentCategory()) {
            case RESIDENCY, FINANCIAL, GUARANTEE_PROVIDER_CERTIFICATE -> "/page1.pdf";
            case PROFESSIONAL -> "/landscapeid.pdf";
            default -> "/CNI.pdf";
        };
        return ValidatedDossierPdfRendererTest.class.getResourceAsStream(file);
    }
}
