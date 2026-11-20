package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Room;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryLine;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Contains the layout choices that CSS cannot make: the size of the cover names, the columns of the summary and the
 * size of the piece images. It decides from the measures of the engine, so it knows no dimension of the stylesheet.
 */
@Value
public class DossierPdfLayout {

    private static final int MAX_NAME_LINES = 2;

    NamesSize namesSize;
    /** The first summary page goes on the cover. The next ones are the "Sommaire, suite" pages. */
    List<SummaryPage> summaryPages;
    @Getter(AccessLevel.NONE)
    Map<String, Room> pieceRooms;

    static DossierPdfLayout of(DossierPdfModel model, DossierPdfMeasures measures) {
        NamesSize namesSize = namesSize(measures);
        List<SummaryPage> summaryPages = new SummaryLayout(measures.covers().get(namesSize).summaryRoom(), measures.nextSummaryRoom(), measures.persons())
                .layOut(model.getCandidates());
        return new DossierPdfLayout(namesSize, summaryPages, measures.pieceRooms());
    }

    /**
     * Returns the largest size that fits the names on two lines. With the smallest size, the names take as many lines
     * as they need. The cover never loses a name.
     */
    private static NamesSize namesSize(DossierPdfMeasures measures) {
        return Arrays.stream(NamesSize.values())
                .filter(size -> measures.covers().get(size).nameLines() <= MAX_NAME_LINES)
                .findFirst()
                .orElse(NamesSize.SMALL);
    }

    /** Returns the largest size of the image that fits the room left by its title, with the same aspect ratio. */
    public ImageSize imageSize(PiecePage page) {
        Room room = pieceRooms.get(page.getTitle());
        float scale = Math.min(room.width() / page.getWidth(), room.height() / page.getHeight());
        return new ImageSize(page.getWidth() * scale, page.getHeight() * scale);
    }

    /** Lists the sizes of the cover names, from the largest to the smallest. The stylesheet defines each class. */
    @Getter
    @RequiredArgsConstructor
    public enum NamesSize {
        LARGE("names-large"),
        MEDIUM("names-medium"),
        SMALL("names-small");

        private final String cssClass;
    }

    /** Gives the displayed size of an image, in points. */
    @Value
    public static class ImageSize {
        float width;
        float height;
    }

    @Value
    @Builder
    public static class SummaryPage {
        SummaryColumn left;
        /** The right column is null when the summary ends in the left one. */
        SummaryColumn right;
    }

    @Value
    @Builder
    public static class SummaryColumn {
        List<SummaryBlock> blocks;
        /** The candidate started in a previous column, so the frame has no top border. */
        boolean openTop;
        /** The candidate continues in the next column, so the frame has no bottom border. */
        boolean openBottom;
        /** Two whole frames side by side take the height of the taller one, in points. Other columns have no set height. */
        Float height;
    }

    /** Describes a person, or the part of a person that fits in a column. */
    @Value
    public static class SummaryBlock {
        SummaryPerson person;
        /** The person started in a previous column. The template then adds ", suite" and leaves the email out. */
        boolean continued;
        List<SummaryLine> lines;
    }
}
