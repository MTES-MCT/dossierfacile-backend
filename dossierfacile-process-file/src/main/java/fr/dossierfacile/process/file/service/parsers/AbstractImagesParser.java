package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.utils.FileUtility;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import fr.dossierfacile.process.file.util.ImageUtils;
import fr.dossierfacile.process.file.util.MemoryUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class AbstractImagesParser<T extends ParsedFile> implements FileParser<T> {
    private transient volatile Tesseract tesseract;

    protected abstract String getJsonModelFile();

    protected abstract T getResultFromExtraction(Map<String, String> extractedText);

    void init() {
        if (tesseract == null) {
            this.tesseract = new Tesseract();
            this.tesseract.setLanguage("fra+digits");
            this.tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            this.tesseract.setVariable("user_defined_dpi", "300");
        }
    }

    @Override
    public T parse(File file) {
        try {
            return parse(ImageUtils.getImagesFromFile(file));
        } catch (IOException e) {
            log.error("Unable to read Image");
            return null;
        }
    }

    protected T parse(BufferedImage... images) {
        try {
            init();
            PageExtractorModel model = new PageExtractorModel(getJsonModelFile());

            BufferedImage image = images[0];
            if (modelMatches(model, image)) {
                Map<String, String> extractedTexts = new HashMap<>();
                double scale = image.getWidth() / model.getDefaultWidth();
                for (Map.Entry<String, Rectangle> entry : model.getNamedZones(scale).entrySet()) {
                    Rectangle rect = entry.getValue();
                    if (image.getWidth() < (rect.x + rect.width)
                            || image.getHeight() < (rect.y + rect.height)) {
                        // rectangle exceeds image size
                        return null;
                    }
                    MemoryUtils.logAvailableMemory(250);
                    String text;
                    synchronized (this) {
                        text = this.tesseract.doOCR(image, rect);
                    }
                    extractedTexts.put(entry.getKey(), text);
                }
                T result = getResultFromExtraction(extractedTexts);
                if (result != null) {
                    enrichWithNextPages(images, result);
                }
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("Unable to parse");
            throw new RuntimeException(e);
        }
    }

    /**
     * If the file has multiple page delegate the enrichment to the inherited class
     */
    protected void enrichWithNextPages(BufferedImage[] images, T result) {
    }

    protected boolean modelMatches(PageExtractorModel model, BufferedImage image) throws IOException {
        double scale = image.getWidth() / model.getDefaultWidth();

        List<PageExtractorModel.Zone> matchingZones = Optional.ofNullable(model.getMatchingZones(scale))
                .orElse(List.of())
                .stream()
                .filter(zone -> zone.pageFilter() == null)
                .toList();
        if (!CollectionUtils.isEmpty(matchingZones)) {
            return matchingZones.stream().allMatch(
                    (zone) -> {
                        try {
                            if (image.getWidth() < (zone.rect().x + zone.rect().width)
                                    || image.getHeight() < (zone.rect().y + zone.rect().height))
                                return false;
                            MemoryUtils.logAvailableMemory(250);
                            String text;
                            synchronized (this) {
                                text = this.tesseract.doOCR(image,  zone.rect());
                            }
                            log.debug("expected: " + zone.regexp() + " actual: " + text + "b=" + (text != null && text.trim().matches(zone.regexp())));
                            return text != null && text.trim().matches(zone.regexp());
                        } catch (Exception e) {
                            return false;
                        }
                    });
        }
        return true;
    }
}
