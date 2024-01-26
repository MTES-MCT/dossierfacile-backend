package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.io.File;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPDFParser<T extends ParsedFile> implements FileParser<T> {

    protected abstract String getJsonModelFile();

    protected abstract T getResultFromExtraction(PDFTextStripperByArea stripper);


    @Override
    public T parse(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDPage page = document.getPage(0);

            PageExtractorModel model = new PageExtractorModel(getJsonModelFile());
            double scale = page.getMediaBox().getWidth() / model.getDefaultWidth();

            if (modelMatches(page, model)) {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                model.getNamedZones(scale).forEach((name, rect) -> stripper.addRegion(name, rect));
                stripper.extractRegions(page);

                return getResultFromExtraction(stripper);
            }
        } catch (Exception e) {
            log.error("Unable to parse");
            throw new RuntimeException(e);
        }
        return null;
    }

    protected boolean modelMatches(PDPage page, PageExtractorModel model) throws IOException {
        return true;
    }
}
