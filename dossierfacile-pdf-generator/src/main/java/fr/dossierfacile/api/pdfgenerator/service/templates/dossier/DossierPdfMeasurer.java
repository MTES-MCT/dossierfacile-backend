package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.PageBox;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryBlock;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryColumn;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryPage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.PersonHeights;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Room;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.Summary;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import lombok.RequiredArgsConstructor;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Measures the blocks of a dossier PDF. The HTML engine lays out {@code draft.html}, which shares its fragments and
 * its stylesheet with the final PDF. This class then reads the positions of the boxes, and the engine prints nothing.
 * <p>
 * No other class depends on the boxes of the engine.
 */
@RequiredArgsConstructor
class DossierPdfMeasurer {

    private static final String TEMPLATE = "draft";
    private static final SummaryPage EMPTY_SUMMARY = page(List.of(), List.of());

    private final DossierHtmlEngine htmlEngine;

    DossierPdfMeasures measure(DossierPdfModel model, DossierPdfResources resources) {
        List<SummaryPerson> persons = model.getCandidates().stream().flatMap(List::stream).distinct().toList();
        List<String> pieceTitles = model.getPiecePages().stream().map(PiecePage::getTitle).distinct().toList();
        Map<String, Object> variables = Map.of(
                "m", model,
                "namesSizes", NamesSize.values(),
                "emptySummary", EMPTY_SUMMARY,
                "noCard", List.of(),
                "personSummaries", persons.stream().map(DossierPdfMeasurer::personSummary).toList(),
                "pieceTitles", pieceTitles);

        try (PdfBoxRenderer renderer = htmlEngine.prepare(TEMPLATE, variables, resources, OutputStream.nullOutputStream()).buildPdfRenderer()) {
            renderer.layout();
            Draft draft = new Draft(renderer.getRootBox(), renderer.getDotsPerPoint());

            Map<NamesSize, Box> covers = new EnumMap<>(NamesSize.class);
            List<Box> coverBoxes = find(draft.root, "cover");
            for (NamesSize size : NamesSize.values()) {
                covers.put(size, coverBoxes.get(size.ordinal()));
            }
            Map<NamesSize, Integer> nameLines = new EnumMap<>(NamesSize.class);
            covers.forEach((size, cover) -> nameLines.put(size, lineCount(first(cover, "band-title-names"))));

            Map<String, Room> pieceRooms = new HashMap<>();
            List<Box> pieces = find(draft.root, "piece");
            for (int index = 0; index < pieceTitles.size(); index++) {
                Box piece = pieces.get(index);
                float width = draft.points(first(piece, "piece-image").getContentWidth());
                pieceRooms.put(pieceTitles.get(index), new Room(width, draft.roomBelow(first(piece, "frame"))));
            }
            Summary summary = model.getStatus() == DossierPdfStatus.VALIDATED ? summary(draft, covers, persons) : null;
            return new DossierPdfMeasures(nameLines, pieceRooms, summary);
        }
    }

    private static Summary summary(Draft draft, Map<NamesSize, Box> covers, List<SummaryPerson> persons) {
        Map<NamesSize, Float> coverRooms = new EnumMap<>(NamesSize.class);
        covers.forEach((size, cover) -> coverRooms.put(size, draft.roomBelow(first(cover, "column"))));
        List<Box> pages = find(draft.root, "summary-page");
        Map<SummaryPerson, PersonHeights> heights = new HashMap<>();
        for (int index = 0; index < persons.size(); index++) {
            heights.put(persons.get(index), draft.personHeights(pages.get(index + 1)));
        }
        return new Summary(coverRooms, draft.roomBelow(first(pages.get(0), "column")), heights);
    }

    /** Displays a person three times: twice in the left column, to measure the gap, and as a continuation in the right one. */
    private static SummaryPage personSummary(SummaryPerson person) {
        SummaryBlock whole = new SummaryBlock(person, false, person.getLines());
        SummaryBlock continued = new SummaryBlock(person, true, person.getLines());
        return page(List.of(whole, whole), List.of(continued));
    }

    private static SummaryPage page(List<SummaryBlock> left, List<SummaryBlock> right) {
        return SummaryPage.builder()
                .left(SummaryColumn.builder().blocks(left).build())
                .right(SummaryColumn.builder().blocks(right).build())
                .build();
    }

    private static Box first(Box box, String cssClass) {
        return find(box, cssClass).get(0);
    }

    /** Returns the boxes of the elements that have the class, in document order. */
    private static List<Box> find(Box box, String cssClass) {
        if (box.getElement() != null && List.of(box.getElement().getAttribute("class").split(" ")).contains(cssClass)) {
            return List.of(box);
        }
        List<Box> found = new ArrayList<>();
        IntStream.range(0, box.getChildCount()).forEach(index -> found.addAll(find(box.getChild(index), cssClass)));
        return found;
    }

    private static int lineCount(Box box) {
        return (int) IntStream.range(0, box.getChildCount()).filter(index -> box.getChild(index) instanceof LineBox).count();
    }

    /** Reads the laid out draft. The engine counts in dots, from the top of the first page and without the page margins. */
    private record Draft(BlockBox root, float dotsPerPoint) {

        float points(int dots) {
            return dots / dotsPerPoint;
        }

        /** Returns the height between the bottom of the box and the bottom of its page. */
        float roomBelow(Box box) {
            int bottom = bottom(box);
            PageBox page = root.getLayer().getPages().stream()
                    .filter(p -> p.getTop() <= bottom && bottom < p.getBottom())
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException("The draft of the dossier PDF leaves no room below " + box.getElement().getAttribute("class")));
            return points(page.getBottom() - bottom);
        }

        PersonHeights personHeights(Box summary) {
            List<Box> persons = find(summary, "person");
            Box whole = persons.get(1);
            Box continued = persons.get(2);
            float gap = points(first(whole, "person-role").getAbsY() - whole.getAbsY());
            List<Float> lines = find(whole, "piece-line").stream().map(line -> points(line.getHeight())).toList();
            return new PersonHeights(gap, headerHeight(whole), headerHeight(continued), lines);
        }

        private float headerHeight(Box person) {
            List<Box> lines = find(person, "piece-line");
            int bottom = lines.isEmpty() ? bottom(person) : lines.get(0).getAbsY();
            return points(bottom - first(person, "person-role").getAbsY());
        }

        private static int bottom(Box box) {
            return box.getAbsY() + box.getHeight();
        }
    }
}
