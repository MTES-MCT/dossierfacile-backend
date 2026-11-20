package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfLayout.NamesSize;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;

import java.util.List;
import java.util.Map;

/**
 * Holds what {@link DossierPdfMeasurer} reads from the HTML engine. All the lengths are in points.
 *
 * @param covers          the cover, for each size of the names
 * @param nextSummaryRoom the height left for the content of a summary column on a "Sommaire, suite" page
 * @param persons         the heights of each person of the summary
 * @param pieceRooms      the room left for the image of a piece, for each piece title
 */
record DossierPdfMeasures(Map<NamesSize, Cover> covers, float nextSummaryRoom, Map<SummaryPerson, PersonHeights> persons,
        Map<String, Room> pieceRooms) {

    /**
     * @param nameLines   the number of lines that the names take
     * @param summaryRoom the height left for the content of a summary column
     */
    record Cover(int nameLines, float summaryRoom) {
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
