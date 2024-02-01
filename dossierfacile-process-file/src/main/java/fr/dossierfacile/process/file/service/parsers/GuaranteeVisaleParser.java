package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.ParsedStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(3)
public class GuaranteeVisaleParser extends AbstractSinglePageImageOcrParser<GuaranteeProviderFile> implements FileParser<GuaranteeProviderFile> {
    static final Zones ZONES = new Zones(
            new Rectangle(120, 150, 370, 30),
            new Rectangle(35, 205, 530, 40),
            new Rectangle(170, 248, 70, 18));
    private final Pattern visaNumberPatten = Pattern.compile("Visa\\s*n.?(V[\\d]+)");
    private final Pattern deliveryDatePattern = Pattern.compile("attribu[éeè] le ([\\d/]+)");
    private final Pattern validityDatePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");

    private transient volatile Tesseract tesseract;

    void init() {
        if (tesseract == null) {
            this.tesseract = new Tesseract();
            this.tesseract.setLanguage("fra+eng");
            this.tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            this.tesseract.setVariable("user_defined_dpi", "128");
        }
    }

    public GuaranteeProviderFile parse(BufferedImage image) {

        GuaranteeProviderFile result = GuaranteeProviderFile.builder().build();
        try {
            init();
            double scale = image.getWidth() / 600; // 600*850 arbitrary width*height chosen for calculate rectangle position

            String zoneTitle = tesseract.doOCR(image, Zones.scale(ZONES.zoneTitle(), scale));
            Matcher visaNumberMatcher = visaNumberPatten.matcher(zoneTitle);
            if (visaNumberMatcher.find()) {
                result.setVisaNumber(visaNumberMatcher.group(1));
            } else {
                log.error("visaNumber not found");
                result.setStatus(ParsedStatus.INCOMPLETE);
            }
            Matcher deliveryDateMatcher = deliveryDatePattern.matcher(zoneTitle);
            if (deliveryDateMatcher.find()) {
                result.setDeliveryDate(deliveryDateMatcher.group(1));
            } else {
                log.error("deliveryDate not found");
                result.setStatus(ParsedStatus.INCOMPLETE);
            }

            String zoneIdentification = tesseract.doOCR(image, Zones.scale(ZONES.zoneIdentification(), scale));
            String[] zonesId = zoneIdentification.split("\n");
            String[] firstNames = zonesId[0].split("Pr[éeè]nom[:\\s\\d]+");
            String[] lastNames = zonesId[1].split("Nom[:\\s\\d]+");
            List<GuaranteeProviderFile.FullName> fullNames = new LinkedList<>();
            for (int i = 1; i < firstNames.length && i < lastNames.length; i++) {
                fullNames.add(new GuaranteeProviderFile.FullName(firstNames[i].trim(), lastNames[i].trim()));
            }
            if (fullNames.isEmpty()) {
                log.error("Firstname/Lastname not found");
                result.setStatus(ParsedStatus.INCOMPLETE);
            }
            result.setNames(fullNames);

            String zoneValidityDate = tesseract.doOCR(image, Zones.scale(ZONES.zoneValidityDate(), scale));
            Matcher validityDateMatcher = validityDatePattern.matcher(zoneValidityDate);
            if (validityDateMatcher.find()) {
                result.setValidityDate(validityDateMatcher.group(1));
            } else {
                log.error("validityDate not found");
                result.setStatus(ParsedStatus.INCOMPLETE);
            }

        } catch (Exception e) {
            log.error("Error during parsing", e);
            result.setStatus(ParsedStatus.INCOMPLETE);
        }
        return result;
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE
                && (file.getDocument().getDocumentSubCategory() == VISALE
                || file.getDocument().getDocumentSubCategory() == OTHER_GUARANTEE));// TODO currently there is not way to selection VISALE subcategory on UI
    }

    public record Zones(Rectangle zoneTitle, Rectangle zoneIdentification, Rectangle zoneValidityDate) {
        public static Rectangle scale(Rectangle rectangle, double scale) {
            return new Rectangle((int) (rectangle.x * scale), (int) (rectangle.y * scale), (int) (rectangle.width * scale), (int) (rectangle.height * scale));
        }
    }
}
