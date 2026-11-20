package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.PersonHeights;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.GuarantorSummary;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryLine;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Lays out the draft with the real engine and stylesheet. The expected heights of the persons come from the Figma. */
class DossierPdfMeasurerTest {

    private static final String MARIE = "Marie-Clémence Vandenberghe-Quéméneur De La Villemarqué";
    private static final String JEAN = "Jean-Baptiste Van Der Straeten-Oudinot De Rosnay";
    private static final String ANNE = "Anne-Laure Bourdonnaye-Lanvaux De Kerangal-Le Bihan";
    private static final float ROUNDING = 0.2f;

    private final DossierPdfMeasurer measurer = new DossierPdfMeasurer(new DossierHtmlEngine());

    @Test
    void should_reduce_the_cover_names_beyond_two_lines() {
        assertThat(namesSize("Camille Lefèvre")).isEqualTo(NamesSize.LARGE);
        assertThat(namesSize(MARIE + " et " + JEAN)).isEqualTo(NamesSize.LARGE);
        assertThat(namesSize("Camille Lefèvre, ".repeat(7) + "Nadia Colin et Thomas Orio")).isEqualTo(NamesSize.MEDIUM);
        assertThat(namesSize("Camille Lefèvre, ".repeat(8) + "Nadia Colin et Thomas Orio")).isEqualTo(NamesSize.SMALL);
        assertThat(namesSize(MARIE + ", " + JEAN + " et " + ANNE)).isEqualTo(NamesSize.SMALL);
    }

    @Test
    void should_measure_the_room_left_by_the_names_and_the_guarantors() {
        float room = summaryRoom("Camille Lefèvre", "Aucun garant");

        assertThat(summaryRoom(MARIE + " et " + JEAN, "Aucun garant")).isCloseTo(room - 22, within(ROUNDING));
        assertThat(summaryRoom("Camille Lefèvre", "Deux personnes", "Un organisme")).isCloseTo(room - 18, within(ROUNDING));
    }

    @Test
    void should_measure_a_guarantor_line_that_wraps() {
        GuarantorSummary wrapped = GuarantorSummary.builder().label("Quatre personnes").detail("(7 500 € cumulés)").build();

        assertThat(measure(model("Camille Lefèvre", List.of(wrapped))).covers().get(NamesSize.LARGE).summaryRoom())
                .isLessThan(summaryRoom("Camille Lefèvre", "Quatre personnes") - 10);
    }

    @Test
    void should_measure_the_persons() {
        SummaryPerson candidate = person(true, "Camille Lefèvre", "camille.lefevre@exemple.fr");
        SummaryPerson guarantor = person(false, "Odile Lefèvre", null);
        SummaryPerson longName = person(false, MARIE, null);
        SummaryPerson longEmail = person(true, "Camille Lefèvre", "a".repeat(120) + "@exemple.fr");

        DossierPdfMeasures measures = measure(model("Camille Lefèvre", List.of()).toBuilder()
                .candidates(List.of(List.of(candidate, guarantor, longName, longEmail)))
                .build());

        PersonHeights heights = measures.persons().get(candidate);
        assertThat(heights.gap()).isCloseTo(12, within(ROUNDING));
        assertThat(height(heights)).isCloseTo(120, within(ROUNDING));
        assertThat(height(measures.persons().get(guarantor))).isCloseTo(101, within(ROUNDING));
        assertThat(heights.lines()).hasSize(5);
        assertThat(heights.header() - heights.continuedHeader()).as("email").isCloseTo(14, within(ROUNDING));
        assertThat(height(measures.persons().get(longName))).as("name on two lines").isCloseTo(101 + 15, within(ROUNDING));
        assertThat(measures.persons().get(longEmail).header()).as("email on three lines").isCloseTo(heights.header() + 22, within(ROUNDING));
    }

    @Test
    void should_measure_the_room_left_by_the_title_of_a_piece() {
        String longTitle = "Le justificatif d'activité professionnelle n° 2 de " + MARIE;
        DossierPdfMeasures measures = measure(model("Camille Lefèvre", List.of()).toBuilder()
                .piecePages(List.of(piece("L'avis d'imposition"), piece(longTitle)))
                .build());

        DossierPdfMeasures.Room room = measures.pieceRooms().get("L'avis d'imposition");
        assertThat(room.width()).isCloseTo(529, within(ROUNDING));
        assertThat(measures.pieceRooms().get(longTitle).height()).isCloseTo(room.height() - 15, within(ROUNDING));
    }

    private NamesSize namesSize(String names) {
        DossierPdfModel model = model(names, List.of());
        return DossierPdfLayout.of(model, measure(model)).getNamesSize();
    }

    private float summaryRoom(String names, String... guarantors) {
        List<GuarantorSummary> summaries = List.of(guarantors).stream().map(label -> GuarantorSummary.builder().label(label).build()).toList();
        return measure(model(names, summaries)).covers().get(NamesSize.LARGE).summaryRoom();
    }

    private DossierPdfMeasures measure(DossierPdfModel model) {
        return measurer.measure(model, new DossierPdfResources());
    }

    private static float height(PersonHeights heights) {
        return heights.header() + heights.lines().stream().reduce(0f, Float::sum);
    }

    private static DossierPdfModel model(String names, List<GuarantorSummary> guarantors) {
        return DossierPdfModel.builder()
                .names(names)
                .validationDate("6 octobre 2026")
                .candidateCount("1 candidat")
                .totalIncome("3 000 €")
                .guarantorSummaries(guarantors)
                .candidates(List.of())
                .piecePages(List.of())
                .build();
    }

    private static SummaryPerson person(boolean candidate, String name, String email) {
        SummaryLine line = SummaryLine.builder().anchor("piece-1").label("Justificatif d'identité").build();
        return SummaryPerson.builder().candidate(candidate).role(candidate ? "Candidat" : "Garant de Camille").name(name).email(email)
                .lines(List.of(line, line, line, line, line)).build();
    }

    private static PiecePage piece(String title) {
        return PiecePage.builder().title(title).width(1240).height(1754).build();
    }
}
