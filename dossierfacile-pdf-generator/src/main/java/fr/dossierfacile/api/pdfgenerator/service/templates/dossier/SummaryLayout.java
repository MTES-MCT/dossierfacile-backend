package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryBlock;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryColumn;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.SummaryPage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfMeasures.PersonHeights;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryLine;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spreads the summary of a validated dossier over two columns per page:
 * <ul>
 *     <li>each candidate starts at the top of a column, followed by its guarantors;</li>
 *     <li>a candidate too long for its column continues in the next one, from the first piece that does not fit.
 *     The next column repeats its role and its name, and the template adds ", suite";</li>
 *     <li>the layout never cuts a piece, and it keeps a name with at least one of its pieces.</li>
 * </ul>
 */
class SummaryLayout {

    private final float firstPageRoom;
    private final float nextPagesRoom;
    private final Map<SummaryPerson, PersonHeights> heights;

    private final List<Column> columns = new ArrayList<>();

    /**
     * @param firstPageRoom the height left for the content of a column on the cover
     * @param nextPagesRoom the height left for the content of a column on the "Sommaire, suite" pages
     */
    SummaryLayout(float firstPageRoom, float nextPagesRoom, Map<SummaryPerson, PersonHeights> heights) {
        this.firstPageRoom = firstPageRoom;
        this.nextPagesRoom = nextPagesRoom;
        this.heights = heights;
    }

    /** @param candidates the persons of each candidate, which are the candidate and then its guarantors */
    List<SummaryPage> layOut(List<List<SummaryPerson>> candidates) {
        candidates.forEach(this::addCandidate);
        List<SummaryPage> pages = new ArrayList<>();
        for (int left = 0; left < columns.size(); left += 2) {
            Column right = left + 1 < columns.size() ? columns.get(left + 1) : null;
            pages.add(page(columns.get(left), right));
        }
        return pages;
    }

    private void addCandidate(List<SummaryPerson> persons) {
        Column column = newColumn(false);
        for (SummaryPerson person : persons) {
            PersonHeights height = heights.get(person);
            float start = height.header() + (height.lines().isEmpty() ? 0 : height.lines().get(0));
            if (!column.isEmpty() && !column.fits(height.gap() + start)) {
                column = cut(column);
            }
            Block block = column.add(person, false, height);
            for (int i = 0; i < person.getLines().size(); i++) {
                if (!block.lines.isEmpty() && !column.fits(height.lines().get(i))) {
                    column = cut(column);
                    block = column.add(person, true, height);
                }
                block.lines.add(person.getLines().get(i));
                column.used += height.lines().get(i);
            }
        }
    }

    private Column newColumn(boolean continuing) {
        Column column = new Column(columns.size() < 2 ? firstPageRoom : nextPagesRoom, continuing);
        columns.add(column);
        return column;
    }

    private Column cut(Column column) {
        column.cut = true;
        return newColumn(true);
    }

    private static SummaryPage page(Column left, Column right) {
        // Two whole frames side by side have the same height.
        boolean sameHeight = right != null && left.isWhole() && right.isWhole();
        Float height = sameHeight ? Math.max(left.used, right.used) : null;
        return SummaryPage.builder()
                .left(left.toModel(height))
                .right(right == null ? null : right.toModel(height))
                .build();
    }

    private static class Column {
        private final float capacity;
        private final boolean continuing;
        private final List<Block> blocks = new ArrayList<>();
        private float used = 0;
        private boolean cut = false;

        Column(float capacity, boolean continuing) {
            this.capacity = capacity;
            this.continuing = continuing;
        }

        boolean isEmpty() {
            return blocks.isEmpty();
        }

        boolean isWhole() {
            return !continuing && !cut;
        }

        boolean fits(float height) {
            return used + height <= capacity;
        }

        Block add(SummaryPerson person, boolean continued, PersonHeights height) {
            used += (isEmpty() ? 0 : height.gap()) + (continued ? height.continuedHeader() : height.header());
            Block block = new Block(person, continued, new ArrayList<>());
            blocks.add(block);
            return block;
        }

        SummaryColumn toModel(Float height) {
            return SummaryColumn.builder()
                    .blocks(blocks.stream().map(b -> new SummaryBlock(b.person, b.continued, List.copyOf(b.lines))).toList())
                    .openTop(continuing)
                    .openBottom(cut)
                    .height(height)
                    .build();
        }
    }

    private record Block(SummaryPerson person, boolean continued, List<SummaryLine> lines) {
    }
}
