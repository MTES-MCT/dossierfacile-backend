package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryBlock;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryColumn;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryPage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.PersonHeights;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryLine;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses round heights, because the layout must not depend on the stylesheet. */
class SummaryLayoutTest {

    private static final int GAP = 10;
    private static final int HEADER = 50;
    private static final int CONTINUED_HEADER = 40;
    private static final int LINE = 10;
    /** A person takes a header and five lines. */
    private static final int PERSON = HEADER + 5 * LINE;
    /** Camille and her two guarantors take this height. */
    private static final int CAMILLE = 3 * PERSON + 2 * GAP;

    @Test
    void should_start_each_candidate_at_the_top_of_a_column() {
        List<SummaryPage> pages = layOut(CAMILLE, 600, camille(), List.of(candidate("Nadia Colin")), List.of(candidate("Inès Martin")));

        assertThat(pages).hasSize(2);
        assertThat(names(pages.get(0).getLeft())).containsExactly("Camille Lefèvre", "Frédéric Lefèvre", "Odile Lefèvre");
        assertThat(names(pages.get(0).getRight())).containsExactly("Nadia Colin");
        assertThat(names(pages.get(1).getLeft())).containsExactly("Inès Martin");
        assertThat(pages.get(1).getRight()).isNull();
    }

    @Test
    void should_give_the_same_height_to_two_whole_frames() {
        SummaryPage page = layOut(CAMILLE, 600, camille(), List.of(candidate("Nadia Colin"))).get(0);

        assertThat(page.getLeft().getHeight()).isEqualTo(CAMILLE);
        assertThat(page.getRight().getHeight()).isEqualTo(CAMILLE);
    }

    @Test
    void should_continue_in_the_next_column_from_the_piece_that_does_not_fit() {
        List<SummaryPage> pages = layOut(CAMILLE - 2 * LINE, 600, camille(), List.of(candidate("Nadia Colin")));

        SummaryColumn left = pages.get(0).getLeft();
        SummaryColumn right = pages.get(0).getRight();
        assertThat(left.isOpenTop()).isFalse();
        assertThat(left.isOpenBottom()).isTrue();
        assertThat(last(left).getLines()).extracting(SummaryLine::getLabel).containsExactly("Pièce 1", "Pièce 2", "Pièce 3");
        assertThat(right.isOpenTop()).isTrue();
        assertThat(right.isOpenBottom()).isFalse();
        assertThat(right.getHeight()).isNull();
        assertThat(right.getBlocks()).singleElement().satisfies(block -> {
            assertThat(block.isContinued()).isTrue();
            assertThat(block.getPerson().getName()).isEqualTo("Odile Lefèvre");
            assertThat(block.getLines()).extracting(SummaryLine::getLabel).containsExactly("Pièce 4", "Pièce 5");
        });
        // The next candidate never starts under another one.
        assertThat(names(pages.get(1).getLeft())).containsExactly("Nadia Colin");
    }

    @Test
    void should_keep_a_name_with_its_first_piece() {
        SummaryPage page = layOut(CAMILLE - 5 * LINE, 600, camille()).get(0);

        assertThat(names(page.getLeft())).containsExactly("Camille Lefèvre", "Frédéric Lefèvre");
        assertThat(page.getLeft().isOpenBottom()).isTrue();
        assertThat(page.getRight().isOpenTop()).isTrue();
        assertThat(last(page.getRight()).isContinued()).isFalse();
        assertThat(last(page.getRight()).getLines()).hasSize(5);
    }

    @Test
    void should_repeat_only_the_role_and_the_name_when_a_candidate_continues() {
        List<SummaryPage> pages = layOut(HEADER + 2 * LINE, 600, List.of(candidate("Camille Lefèvre")), List.of(candidate("Nadia Colin")));

        assertThat(columns(pages)).extracting(column -> last(column).getLines().size()).containsExactly(2, 3, 5);
        assertThat(columns(pages)).extracting(column -> last(column).isContinued()).containsExactly(false, true, false);
    }

    @Test
    void should_continue_over_several_pages() {
        List<SummaryPage> pages = layOut(HEADER + LINE, CONTINUED_HEADER + LINE, List.of(candidate("Camille Lefèvre")));

        assertThat(pages).hasSize(2);
        assertThat(columns(pages)).extracting(column -> last(column).getLines().size()).containsExactly(1, 2, 1, 1);
        assertThat(columns(pages)).extracting(column -> last(column).isContinued()).containsExactly(false, true, true, true);
    }

    @Test
    void should_keep_a_person_without_piece() {
        SummaryPerson person = SummaryPerson.builder().candidate(true).role("Candidat").name("Camille Lefèvre").lines(List.of()).build();

        SummaryPage page = layOut(CAMILLE, 600, List.of(person)).get(0);

        assertThat(names(page.getLeft())).containsExactly("Camille Lefèvre");
    }

    @Test
    void should_lose_no_piece_and_never_exceed_a_column() {
        for (int room = PERSON; room < 500; room++) {
            List<SummaryPage> pages = layOut((float) room, (float) room, camille(), List.of(candidate("Nadia Colin")), camille());

            assertThat(columns(pages).stream().flatMap(c -> c.getBlocks().stream()).mapToInt(b -> b.getLines().size()).sum()).isEqualTo(35);
            int capacity = room;
            assertThat(columns(pages)).allSatisfy(column -> assertThat(height(column)).isLessThanOrEqualTo(capacity));
        }
    }

    private static int height(SummaryColumn column) {
        return column.getBlocks().stream()
                .mapToInt(block -> (block.isContinued() ? CONTINUED_HEADER : HEADER) + block.getLines().size() * LINE)
                .sum() + (column.getBlocks().size() - 1) * GAP;
    }

    @SafeVarargs
    private static List<SummaryPage> layOut(float first, float next, List<SummaryPerson>... candidates) {
        Map<SummaryPerson, PersonHeights> heights = List.of(candidates).stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toMap(Function.identity(),
                        person -> new PersonHeights(GAP, HEADER, CONTINUED_HEADER, Collections.nCopies(person.getLines().size(), (float) LINE))));
        return new SummaryLayout(first, next, heights).layOut(List.of(candidates));
    }

    private static List<SummaryPerson> camille() {
        return List.of(candidate("Camille Lefèvre"), guarantor("Frédéric Lefèvre"), guarantor("Odile Lefèvre"));
    }

    private static SummaryPerson candidate(String name) {
        return SummaryPerson.builder().candidate(true).role("Candidat").name(name).email("camille.lefevre@exemple.fr").lines(lines()).build();
    }

    private static SummaryPerson guarantor(String name) {
        return SummaryPerson.builder().role("Garant de Camille").name(name).lines(lines()).build();
    }

    private static List<SummaryLine> lines() {
        return IntStream.rangeClosed(1, 5).mapToObj(i -> SummaryLine.builder().anchor("piece-" + i).label("Pièce " + i).build()).toList();
    }

    private static List<SummaryColumn> columns(List<SummaryPage> pages) {
        List<SummaryColumn> columns = new ArrayList<>();
        pages.forEach(page -> {
            columns.add(page.getLeft());
            if (page.getRight() != null) {
                columns.add(page.getRight());
            }
        });
        return columns;
    }

    private static List<String> names(SummaryColumn column) {
        return column.getBlocks().stream().map(block -> block.getPerson().getName()).toList();
    }

    private static SummaryBlock last(SummaryColumn column) {
        return column.getBlocks().get(column.getBlocks().size() - 1);
    }
}
