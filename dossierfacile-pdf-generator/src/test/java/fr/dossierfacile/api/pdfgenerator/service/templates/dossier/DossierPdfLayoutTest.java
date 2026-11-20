package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.ImageSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Cover;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Room;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
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

    private static PiecePage page(String title, float width, float height) {
        return PiecePage.builder().title(title).width(width).height(height).build();
    }

    private static DossierPdfLayout layout(int largeLines, int mediumLines, int smallLines) {
        Map<NamesSize, Cover> covers = Map.of(
                NamesSize.LARGE, new Cover(largeLines, 300),
                NamesSize.MEDIUM, new Cover(mediumLines, 300),
                NamesSize.SMALL, new Cover(smallLines, 300));
        Map<String, Room> pieceRooms = Map.of("Portrait", new Room(500, 600), "Long title", new Room(500, 500));
        return DossierPdfLayout.of(DossierPdfModel.builder().candidates(List.of()).build(), new DossierPdfMeasures(covers, 500, Map.of(), pieceRooms));
    }
}
