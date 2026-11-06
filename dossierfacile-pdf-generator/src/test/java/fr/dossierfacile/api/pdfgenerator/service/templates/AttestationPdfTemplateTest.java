package fr.dossierfacile.api.pdfgenerator.service.templates;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AttestationPdfTemplateTest {

    // Generated PDFs are kept there to be checked visually
    static final Path OUTPUT_DIR = Paths.get("target", "attestations");
    // 23:30 UTC on the 5th is already the 6th in France
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"), ZoneOffset.UTC);
    static final String TODAY = "06/11/2026";

    private static final String STATEMENT = "Je ne perçois pas de revenus.";
    // Too long for one line of the page
    private static final String LONG_NAME = "Marie-Charlotte Alexandrine de La Rochefoucauld-Montmorency de Bourbon-Parme";
    private static final float RIGHT_MARGIN = PDRectangle.A4.getWidth() - 40;

    private final AttestationPdfTemplate template = template();

    static AttestationPdfTemplate template() {
        AttestationPdfTemplate template = new AttestationPdfTemplate(messageSource(), (document, outputStream) -> document.save(outputStream));
        template.setClock(CLOCK);
        return template;
    }

    static MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    static byte[] save(String fileName, InputStream pdf) throws IOException {
        byte[] bytes = pdf.readAllBytes();
        Files.createDirectories(OUTPUT_DIR);
        Files.write(OUTPUT_DIR.resolve(fileName), bytes);
        return bytes;
    }

    static String textOf(PDDocument document) throws IOException {
        return StringUtils.normalizeSpace(new PDFTextStripper().getText(document));
    }

    private static String textOfPage(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return StringUtils.normalizeSpace(stripper.getText(document));
    }

    // Right edge of the rightmost character of the document
    private static float rightEdgeOf(PDDocument document) throws IOException {
        float[] rightEdge = {0};
        new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                rightEdge[0] = Math.max(rightEdge[0], text.getXDirAdj() + text.getWidthDirAdj());
                super.processTextPosition(text);
            }
        }.getText(document);
        return rightEdge[0];
    }

    @Test
    void should_render_the_statement_on_a_single_a4_page() throws Exception {
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, "Je suis étudiante à temps plein, sans emploi ni bourse.");

        byte[] pdf = save("statement-with-explanation.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            PDRectangle mediaBox = document.getPage(0).getMediaBox();
            assertThat(mediaBox.getWidth()).isCloseTo(PDRectangle.A4.getWidth(), within(0.1f));
            assertThat(mediaBox.getHeight()).isCloseTo(PDRectangle.A4.getHeight(), within(0.1f));
            assertThat(textOf(document)).isEqualTo("Attestation sur l’honneur "
                    + "Je soussigné·e Dr Who, atteste sur l’honneur que : "
                    + "\"Je ne perçois pas de revenus.\" "
                    + "Précisions : Je suis étudiante à temps plein, sans emploi ni bourse. "
                    + "Fait le " + TODAY + " "
                    + "Signature "
                    + "Dr Who");
        }
    }

    @Test
    void should_not_render_a_blank_explanation() throws Exception {
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, " ");

        byte[] pdf = save("statement-only.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("\"Je ne perçois pas de revenus.\" Fait le " + TODAY);
        }
    }

    @Test
    void should_flatten_line_breaks() throws Exception {
        AttestationContent content = new AttestationContent("Dr\tWho", STATEMENT, "Première ligne.\r\n\r\nSeconde ligne.");

        byte[] pdf = save("statement-with-line-breaks.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("Je soussigné·e Dr Who,", "Précisions : Première ligne. Seconde ligne.");
        }
    }

    @Test
    void should_add_pages_when_the_text_is_too_long() throws Exception {
        String explanation = StringUtils.repeat("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ", 35).trim();
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, explanation);

        byte[] pdf = save("long-explanation.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(textOf(document)).contains("Précisions : " + explanation);
            assertThat(rightEdgeOf(document)).isLessThanOrEqualTo(RIGHT_MARGIN);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dr Who", LONG_NAME})
    void should_keep_the_date_the_signature_and_the_name_on_the_same_page(String name) throws Exception {
        String closingBlock = "Fait le " + TODAY + " Signature " + name;
        boolean closingBlockMovedToANewPage = false;

        // Every explanation length around the end of the first page
        for (int words = 30; words <= 110; words++) {
            AttestationContent content = new AttestationContent(name, STATEMENT, StringUtils.repeat("précision", " ", words));

            try (PDDocument document = Loader.loadPDF(template.render(content).readAllBytes())) {
                int lastPage = document.getNumberOfPages();
                String lastPageText = textOfPage(document, lastPage);
                assertThat(lastPageText).as("%d words", words).endsWith(closingBlock);
                if (lastPage > 1) {
                    assertThat(textOfPage(document, lastPage - 1)).as("%d words", words).doesNotContain("Fait le", "Signature");
                    closingBlockMovedToANewPage |= lastPageText.equals(closingBlock);
                }
            }
        }

        assertThat(closingBlockMovedToANewPage).isTrue();
    }

    @Test
    void should_cut_a_word_wider_than_the_page() throws Exception {
        String email = "prenom.nom-compose.de-tres-grande-longueur@administration-exemple.gouv.fr";
        String url = "https://www.exemple.gouv.fr/" + StringUtils.repeat("chemin-tres-long/", 8) + "document.pdf";
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, "Contact : " + email + " ou " + url + " merci.");

        byte[] pdf = save("statement-with-long-words.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(rightEdgeOf(document)).isLessThanOrEqualTo(RIGHT_MARGIN);
            assertThat(StringUtils.deleteWhitespace(new PDFTextStripper().getText(document))).contains(email, url);
        }
    }

    @Test
    void should_not_break_the_line_before_a_colon() throws Exception {
        // With this name, "que :" does not fit at the end of the first line
        AttestationContent content = new AttestationContent("Dominique Martin", STATEMENT, null);

        byte[] pdf = save("statement-with-long-name.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document).lines().map(String::trim))
                    .contains("Je soussigné·e Dominique Martin, atteste sur l’honneur", "que :")
                    .doesNotContain(":");
        }
    }

    @Test
    void should_not_break_the_line_before_a_colon_of_the_statement() throws Exception {
        // One of these lengths puts the colon at the end of a line
        for (int words = 1; words <= 30; words++) {
            String statement = StringUtils.repeat("mot", " ", words) + " exemple : suite";

            try (PDDocument document = Loader.loadPDF(template.render(new AttestationContent("Dr Who", statement, null)).readAllBytes())) {
                assertThat(new PDFTextStripper().getText(document).lines().map(String::trim))
                        .as("%d words", words)
                        .noneMatch(line -> line.startsWith(":"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nguyễn Thị Hương", "Trần Văn Đạt", "Иван Петров", "Ελένη Παππά", "Søren Žižek-Łukasiewicz"})
    void should_render_names_of_other_alphabets(String name) throws Exception {
        byte[] pdf = save("name-" + name.replace(' ', '-') + ".pdf", template.render(new AttestationContent(name, STATEMENT, null)));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("Je soussigné·e " + name + ", atteste", "Signature " + name);
        }
    }

    @Test
    void should_render_emojis() throws Exception {
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, "Je suis en voyage 😁 depuis un an.");

        byte[] pdf = save("statement-with-emoji.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("Je suis en voyage 😁 depuis un an.");
        }
    }

    @Test
    void should_skip_a_character_missing_from_every_font() throws Exception {
        AttestationContent content = new AttestationContent("Dr Who", STATEMENT, "Avant ꀀ après.");

        byte[] pdf = save("statement-with-unknown-character.pdf", template.render(content));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("Précisions : Avant après.");
        }
    }

    @Test
    void should_render_without_name() throws Exception {
        byte[] pdf = save("statement-without-name.pdf", template.render(new AttestationContent(null, STATEMENT, null)));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(textOf(document)).contains("\"Je ne perçois pas de revenus.\" Fait le " + TODAY + " Signature");
        }
    }

    @Test
    void should_refuse_an_attestation_without_statement() {
        assertThatThrownBy(() -> new AttestationContent("Dr Who", " ", null)).isInstanceOf(IllegalArgumentException.class);
    }

}
