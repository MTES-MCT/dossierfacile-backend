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
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPDFParser<T extends ParsedFile> implements FileParser<T> {

    protected final PageExtractorModel model;

    AbstractPDFParser() {
        try {
            model = new PageExtractorModel(getJsonModelFile());
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize Parser - check code");
        }
    }

    protected abstract String getJsonModelFile();

    protected abstract T getResultFromExtraction(PDFTextStripperByArea stripper, T previousResult);

    @Override
    public T parse(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            int pageCount = document.getPages().getCount();
            if (model.getMaxPageCount() < pageCount) {
                return null;
            }
            T result = null;
            for (int i = 0; i < pageCount; i++) {
                if (getPageParser(i) != null) {
                    result = getPageParser(i).parsePage(document.getPage(i), result);
                    if (result == null)
                        break;
                }
            }
            return result;

        } catch (Exception e) {
            log.error("Unable to parse", e);
            throw new RuntimeException(e);
        }
    }

    protected AbstractPDFParser<T> getPageParser(int i) {
        if (i == 0) {
            return this;
        }
        return null;
    }

    private T parsePage(PDPage page, T result) throws IOException {
        double scale = page.getMediaBox().getWidth() / model.getDefaultWidth();

        if (modelMatches(model, page)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            model.getNamedZones(scale).forEach((name, rect) -> stripper.addRegion(name, rect));
            stripper.extractRegions(page);
            result = getResultFromExtraction(stripper, result);
        }
        return result;
    }


    protected boolean modelMatches(PageExtractorModel model, PDPage page) throws IOException {
        double scale = page.getMediaBox().getWidth() / model.getDefaultWidth();

        List<PageExtractorModel.Zone> matchingZones = Optional.ofNullable(model.getMatchingZones(scale))
                .orElse(List.of())
                .stream()
                .filter(zone -> zone.pageFilter() == null)
                .toList();
        if (!CollectionUtils.isEmpty(matchingZones)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            matchingZones.forEach((zone) -> stripper.addRegion(zone.name(), zone.rect()));
            stripper.extractRegions(page);
            return model.getMatchingZones(scale).stream()
                    .allMatch((zone) -> {
                                String text = stripper.getTextForRegion(zone.name());
                                if (log.isDebugEnabled()) {
                                    if (!(text != null && text.trim().matches(zone.regexp()))) {
                                        log.debug(text.trim() + " != " + zone.regexp());
                                    }
                                }
                                return text != null && text.trim().matches(zone.regexp());
                            }
                    );
        }
        return true;
    }
}
