package fr.dossierfacile.process.file.service.ocr;


import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaxIncomeLeafParser extends AbstractSinglePageImageOcrParser<TaxIncomeLeaf> implements OcrParser<TaxIncomeLeaf> {

    static final TaxIncomeLeafParser.TaxIncomeLeafZones TEMPLATE_2023 =
            new TaxIncomeLeafParser.TaxIncomeLeafZones(
                    new Rectangle(10, 23, 180, 35),
                    new Rectangle(375, 23, 195, 35));

    private final Pattern incomeYearPattern = Pattern.compile("revenus de (\\d{4})");
    private final Pattern fiscalNumberPattern = Pattern.compile("fiscal : ([\\d \\s]+)");
    private final Pattern pageInfoPattern = Pattern.compile("Feuillet n.? : (\\d)[/\\s](\\d)");

    private transient volatile Tesseract tesseract;

    void init() {
        if (tesseract == null) {
            this.tesseract = new Tesseract();
            this.tesseract.setLanguage("fra+eng");
            this.tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            this.tesseract.setVariable("user_defined_dpi", "300");
        }
    }

    public TaxIncomeLeaf parse(BufferedImage image) {

        TaxIncomeLeaf result = TaxIncomeLeaf.builder().build();
        try {
            init();
            double scale = image.getWidth() / 600; // 600*850 arbitrary width*height chosen for calculate rectangle position

            for (TaxIncomeLeafZones zones : List.of(TEMPLATE_2023)) {
                String zoneHeaderLeft = tesseract.doOCR(image, TaxIncomeLeafZones.scale(zones.headerLeft, scale));
                Matcher matcher = fiscalNumberPattern.matcher(zoneHeaderLeft);
                if (matcher.find()) {
                    result.setNumeroFiscal(matcher.group(1));
                } else {
                    log.error("Numero Fiscal not found");
                    continue;
                }

                String zoneHeaderRight = tesseract.doOCR(image, TaxIncomeLeafZones.scale(zones.headerRight, scale));
                Matcher pageInfo = pageInfoPattern.matcher(zoneHeaderRight);
                try {
                    if (pageInfo.find()) {
                        try {
                            result.setPage(Integer.parseInt(pageInfo.group(1)));
                            result.setPageCount(Integer.parseInt(pageInfo.group(2)));
                        } catch (NumberFormatException pageCountException) {
                            log.error("Unable to parse to integer", pageCountException);
                        }
                    } else {
                        log.error("\"Information de feuillets\" not found " + zoneHeaderRight);
                        continue;
                    }
                } catch (Exception e) {
                    log.error("\"Information de feuillets\" error");
                    continue;
                }
                Matcher yearMatcher = incomeYearPattern.matcher(zoneHeaderRight);
                if (yearMatcher.find()) {
                    result.setAnneeDesRevenus(yearMatcher.group(1));
                    break;
                } else {
                    log.warn("\"Année des revenus\" not found" + zoneHeaderRight);
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("Error during parsing", e);
        }
        return result;

    }

    public record TaxIncomeLeafZones(Rectangle headerLeft, Rectangle headerRight) {
        public static Rectangle scale(Rectangle rectangle, double scale) {
            return new Rectangle((int) (rectangle.x * scale), (int) (rectangle.y * scale), (int) (rectangle.width * scale), (int) (rectangle.height * scale));
        }
    }
}
