package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.ImageSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Room;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DossierPdfLayoutTest {

    @Test
    void should_choose_the_largest_size_that_fits_the_names_on_two_lines() {
        assertThat(layout(1, 1, 1).getNamesSize()).isEqualTo(NamesSize.LARGE);
        assertThat(layout(2, 2, 1).getNamesSize()).isEqualTo(NamesSize.LARGE);
        assertThat(layout(3, 2, 2).getNamesSize()).isEqualTo(NamesSize.MEDIUM);
        assertThat(layout(3, 3, 2).getNamesSize()).isEqualTo(NamesSize.SMALL);
    }

    @Test
    void should_accept_more_lines_with_the_smallest_size() {
        assertThat(layout(6, 5, 4).getNamesSize()).isEqualTo(NamesSize.SMALL);
    }

    @Test
    void should_fit_an_image_in_the_room_left_by_its_title() {
        DossierPdfLayout layout = layout(1, 1, 1);

        assertThat(layout.imageSize(page("Portrait", 1000, 2000))).isEqualTo(new ImageSize(300, 600));
        assertThat(layout.imageSize(page("Portrait", 2000, 1000))).isEqualTo(new ImageSize(500, 250));
        assertThat(layout.imageSize(page("Long title", 1000, 2000))).isEqualTo(new ImageSize(250, 500));
    }

    @Test
    void should_start_a_row_of_cards_for_each_candidate() {
        List<SummaryPerson> camille = List.of(person("Camille"), person("Frédéric"), person("Odile"), person("Hélène"));
        List<SummaryPerson> nadia = List.of(person("Nadia"));

        DossierPdfLayout layout = layout(DossierPdfModel.builder().candidates(List.of(camille, nadia)).build(), 1, 1, 1);

        assertThat(layout.getCardRows()).containsExactly(camille.subList(0, 3), camille.subList(3, 4), nadia);
        assertThat(layout.getSummaryPages()).isEmpty();
    }

    private static SummaryPerson person(String name) {
        return SummaryPerson.builder().name(name).build();
    }

    private static PiecePage page(String title, float width, float height) {
        return PiecePage.builder().title(title).width(width).height(height).build();
    }

    private static DossierPdfLayout layout(int largeLines, int mediumLines, int smallLines) {
        return layout(DossierPdfModel.builder().candidates(List.of()).build(), largeLines, mediumLines, smallLines);
    }

    private static DossierPdfLayout layout(DossierPdfModel model, int largeLines, int mediumLines, int smallLines) {
        Map<NamesSize, Integer> nameLines = Map.of(NamesSize.LARGE, largeLines, NamesSize.MEDIUM, mediumLines, NamesSize.SMALL, smallLines);
        Map<String, Room> pieceRooms = Map.of("Portrait", new Room(500, 600), "Long title", new Room(500, 500));
        return DossierPdfLayout.of(model, new DossierPdfMeasures(nameLines, pieceRooms, null));
    }
}
