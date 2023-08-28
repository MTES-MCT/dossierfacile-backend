package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class PdfTextExtractor {

    private final PdfPage pdfPage;

    private PageTextStripper textStripper;
    private String documentText;

    List<TextPosition> findText(String text) {
        return getTextStripper()
                .map(textStripper -> textStripper.findText(text))
                .orElseGet(List::of);
    }

    boolean documentContainsText() {
        return !getDocumentText().isBlank();
    }

    String getDocumentText() {
        if (documentText != null) {
            return documentText;
        }
        return getTextStripper()
                .map(PageTextStripper::getText)
                .orElse("");
    }

    private Optional<PageTextStripper> getTextStripper() {
        if (textStripper == null) {
            try {
                textStripper = new PageTextStripper(pdfPage);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.of(textStripper);
    }

    private class PageTextStripper extends PDFTextStripper {

        private String searchedText;
        private List<TextPosition> foundTextPositions;

        public PageTextStripper(PdfPage pdfPage) throws IOException {
            super();
            this.document = pdfPage.document();
            setSortByPosition(true);

            int oneBasedIndex = pdfPage.pageIndex() + 1;
            setStartPage(oneBasedIndex);
            setEndPage(oneBasedIndex);
        }

        List<TextPosition> findText(String text) {
            this.searchedText = text;
            this.foundTextPositions = new ArrayList<>();

            documentText = getText();

            return foundTextPositions;
        }

        public String getText() {
            try {
                return super.getText(document);
            } catch (IOException e) {
                return "";
            }
        }

        @Override
        // Called by getText()
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (searchedText != null && string.contains(searchedText)) {
                TextPosition centralPosition = textPositions.get(textPositions.size() / 2);
                foundTextPositions.add(centralPosition);
            }
            super.writeString(string);
        }

    }

}
