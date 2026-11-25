package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Contains everything that the dossier PDF templates display. The builder computes and formats each value, so the
 * templates contain no business rule.
 */
@Value
@Builder(toBuilder = true)
public class DossierPdfModel {

    DossierPdfStatus status;
    /** The document and the PDF metadata use this title, for example "Dossier de location de Camille Lefèvre". */
    String title;
    /** This text joins the full names of all the candidates, as in "A, B et C". */
    String names;
    /** This is the validation date of a validated dossier, or else the deposit date of the pieces, for example "6 octobre 2026". */
    String date;
    /** This is the number of candidates, for example "1 candidat" or "3 candidats". */
    String candidateCount;
    /** This is the sum of the net monthly incomes of the candidates, for example "9 600 €". */
    String totalIncome;
    /** The list has one line per guarantor type, or a single "Aucun garant" line. */
    List<GuarantorSummary> guarantorSummaries;
    /** This list gives the persons of each candidate, which are the candidate and then its guarantors. */
    List<List<SummaryPerson>> candidates;
    /** This title is "Le mot du candidat" or "Le mot des candidats". */
    String clarificationTitle;
    /** The main candidate wrote this free text. It is null when the candidate wrote nothing. */
    String clarification;
    /** The list has one entry per page of every piece, in document order. */
    List<PiecePage> piecePages;
    List<Bookmark> bookmarks;

    /** Tells whether the texts about the candidates are in the plural. */
    public boolean isPlural() {
        return candidates.size() > 1;
    }

    @Value
    @Builder
    public static class GuarantorSummary {
        /** This label counts the guarantors of a type, for example "Deux personnes" or "Un organisme". */
        String label;
        /** This detail gives their income, for example "(10 700 € cumulés)". It is null when there is none. */
        String detail;
    }

    /** Describes a person of the summary. */
    @Value
    @Builder
    public static class SummaryPerson {
        /** The person is a candidate, not a guarantor. */
        boolean candidate;
        /** The role reads "Candidat" or "Garant de Camille". */
        String role;
        String name;
        /** The email is null for a guarantor. */
        String email;
        /** This counts the documents of the person, for example "5 pièces" or "1 pièce". */
        String pieceCount;
        List<SummaryLine> lines;
    }

    @Value
    @Builder
    public static class SummaryLine {
        /** This anchor points to the first page of the piece. The template prints its page number. */
        String anchor;
        /** This label names the piece, for example "Justificatif d'identité". */
        String label;
        /** This is the sum of the net amounts of the income pieces, for example "3 000 € net". It is null for other pieces. */
        String amount;
    }

    @Value
    @Builder
    public static class PiecePage {
        String anchor;
        /** This title names the piece and its owner, for example "La pièce d'identité de Camille Lefèvre". */
        String title;
        /** This URI points to a JPEG image, or to the PDF that contains the page {@link #pdfPage}. */
        String uri;
        /** This is the page number in the PDF. It is null for an image. */
        Integer pdfPage;
        String alt;
        /** This is the original size of the page. The unit is free, because only the aspect ratio matters. */
        float width;
        float height;
    }

    @Value
    @Builder
    public static class Bookmark {
        String name;
        String anchor;
        List<Bookmark> children;
    }
}
