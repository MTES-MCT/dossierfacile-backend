package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPDFParser<T extends ParsedFile> implements FileParser<T> {

    protected abstract String getJsonModelFile();

    protected abstract T getResultFromExtraction(PDFTextStripperByArea stripper, int pageNumber, T previousResult);


    @Override
    public T parse(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {

            int pageCount = document.getPages().getCount();
            PageExtractorModel model = new PageExtractorModel(getJsonModelFile());
            if (model.getMaxPageCount() < pageCount) {
                return null;
            }
            T result = null;
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                double scale = page.getMediaBox().getWidth() / model.getDefaultWidth();

                if (modelMatches(model, page, i)) {
                    PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                    model.getNamedZones(scale).forEach((name, rect) -> stripper.addRegion(name, rect));
                    stripper.extractRegions(page);

                    result = getResultFromExtraction(stripper, i, result);
                }
            }
            return result;

        } catch (Exception e) {
            log.error("Unable to parse");
            throw new RuntimeException(e);
        }
    }

    protected boolean modelMatches(PageExtractorModel model, PDPage page, int pageNumber) throws IOException {
        double scale = page.getMediaBox().getWidth() / model.getDefaultWidth();

        List<PageExtractorModel.Zone> matchingZones = model.getMatchingZones(scale).stream()
                .filter(zone -> zone.pageFilter() == null)
                .toList();
        if (!CollectionUtils.isEmpty(matchingZones)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            matchingZones.forEach((zone) -> stripper.addRegion(zone.name(), zone.rect()));
            stripper.extractRegions(page);
            return model.getMatchingZones(scale).stream()
                    .allMatch((zone) -> {
                                String text = stripper.getTextForRegion(zone.name());
                                return text != null && text.trim().matches(zone.regexp());
                            }
                    );
        }
        return true;
    }
}
