package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;

import java.util.List;
import java.util.Map;

/**
 * Holds what {@link DossierPdfMeasurer} reads from the HTML engine. All the lengths are in points.
 *
 * @param nameLines  the number of lines that the names of the cover take, for each size
 * @param pieceRooms the room left for the image of a piece, for each piece title
 * @param summary    the measures of the summary, which is null when the dossier has no summary
 */
record DossierPdfMeasures(Map<NamesSize, Integer> nameLines, Map<String, Room> pieceRooms, Summary summary) {

    /**
     * @param coverRooms the height left for the content of a column on the cover, for each size of the names
     * @param nextRoom   the height left for the content of a column on a "Sommaire, suite" page
     * @param persons    the heights of each person
     */
    record Summary(Map<NamesSize, Float> coverRooms, float nextRoom, Map<SummaryPerson, PersonHeights> persons) {
    }

    /**
     * @param gap             the space above a person, unless the person is the first one of its column
     * @param header          the height of the role, the name and the email
     * @param continuedHeader the height of the role with ", suite" and the name
     * @param lines           the height of each piece
     */
    record PersonHeights(float gap, float header, float continuedHeader, List<Float> lines) {
    }

    record Room(float width, float height) {
    }
}
