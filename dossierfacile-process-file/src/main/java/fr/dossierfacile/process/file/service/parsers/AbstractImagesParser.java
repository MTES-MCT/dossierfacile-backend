package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.utils.FileUtility;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
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
    private BufferedImage[] getImages(File file) throws IOException {
        if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
            BufferedImage[] images = FileUtility.convertPdfToImage(file);
            if (images == null || images.length < 1) {
                throw new IllegalStateException("pdf file cannot be convert to images");
            }
            return images;
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalStateException("image cannot be extracted from file " + file.getName());
        }
        return new BufferedImage[]{image};
    }

    @Override
    public T parse(File file) {
        try {
            return parse(getImages(file));
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
                    extractedTexts.put(entry.getKey(), this.tesseract.doOCR(image, rect));
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

                            String text = this.tesseract.doOCR(image, zone.rect());
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
