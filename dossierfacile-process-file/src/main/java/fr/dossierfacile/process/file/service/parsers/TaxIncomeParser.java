package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.utils.FileUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class TaxIncomeParser implements FileParser<TaxIncomeMainFile> {
    static final TaxIncomeZones TEMPLATE_2023 = new TaxIncomeZones(new Rectangle(120, 10, 400, 65), new Rectangle(17, 176, 200, 180), new Rectangle(250, 170, 300, 100), new Rectangle(226, 595, 325, 55));
    static final TaxIncomeZones TEMPLATE_2020 = new TaxIncomeZones(new Rectangle(140, 40, 325, 65), new Rectangle(40, 155, 200, 180), new Rectangle(270, 130, 300, 100), new Rectangle(255, 570, 315, 55));
    private final Pattern incomeYearPattern = Pattern.compile("revenus de (\\d{4})");
    private final Pattern incomeAmountPattern = Pattern.compile("Revenu fiscal de référence : ([\\dOlI|S \\s]+)");
    private final Pattern partCountPattern = Pattern.compile("Nombre de parts : ([\\d\\s,]+)");
    private final Pattern declarant2NameInAddressPattern = Pattern.compile("OU (.+)\n");
    private final Pattern declarant1Pattern = Pattern.compile("Déclarant 1 \\(C\\): ([\\s\\w]+)|Numéro fiscal \\(C\\) : ([\\s\\w]+)|");

    private final TaxIncomeLeafParser taxIncomeLeafParser;
    private transient volatile Tesseract tesseract;




    String fixNumber(String str){
        return str.replaceAll("O", "0").replaceAll("[lI|]", "1").replaceAll("S", "5").replaceAll("\\s", "");
    }
    String fixLetter(String str){
        return str.replaceAll("0", "O").replaceAll("5", "S").trim();
    }
    void init() {
        if (tesseract == null) {
            this.tesseract = new Tesseract();
            this.tesseract.setLanguage("fra+eng");
            this.tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            this.tesseract.setVariable("user_defined_dpi", "300");
        }
    }

    private BufferedImage getImage(File file, TaxIncomeMainFile taxIncomeMainFile) throws IOException {
        BufferedImage image;
        if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {

            BufferedImage[] images = FileUtility.convertPdfToImage(file);
            if (images == null || images.length == 0) {
                throw new IllegalStateException("pdf file cannot be convert to images");
            }
            image = images[0];
            if (images.length > 1) {
                taxIncomeMainFile.setTaxIncomeLeaves(new LinkedList<>());
                // parse leaf
                for (int i = 1; i < images.length; i++) {
                    taxIncomeMainFile.getTaxIncomeLeaves().add(taxIncomeLeafParser.parse(images[i]));
                }
            }
        } else {
            image = ImageIO.read(file);
        }

        if (image == null) {
            throw new IllegalStateException("image cannot be extracted from file " + file.getName());
        }
        return image;
    }

    public TaxIncomeMainFile parse(File file) {
        TaxIncomeMainFile result = TaxIncomeMainFile.builder().build();
        try {
            init();
            BufferedImage image = getImage(file, result);
            double scale = image.getWidth() / 600; // 600*850 arbitrary width*height chosen for calculate rectangle position

            for (TaxIncomeZones zones : Arrays.asList(TEMPLATE_2023, TEMPLATE_2020)) {
                String zoneTitle = tesseract.doOCR(image, TaxIncomeZones.scale(zones.zoneTitle, scale));
                Matcher matcher = incomeYearPattern.matcher(zoneTitle);
                if (matcher.find()) {
                    result.setAnneeDesRevenus(Integer.parseInt(fixNumber(matcher.group(1))));
                } else {
                    log.warn("Income year not found");
                    continue; // not need to continue the parsing is failed
                }
                String zoneRef = tesseract.doOCR(image, TaxIncomeZones.scale(zones.zoneRef, scale));
                Matcher declarant1Matcher = declarant1Pattern.matcher(zoneRef);
                if (declarant1Matcher.find()) {
                    result.setDeclarant1NumFiscal(declarant1Matcher.group(1));
                } else {
                    log.info("\"Déclarant 1 fiscal number\" not found \n");
                }
                String zoneAddress = tesseract.doOCR(image, TaxIncomeZones.scale(zones.zoneAddress, scale));
                String declarant1Name = zoneAddress.substring(0, zoneAddress.indexOf('\n'));
                if (declarant1Name != null) {
                    result.setDeclarant1Nom(fixLetter(declarant1Name));
                } else {
                    log.warn("\"Déclarant 1\" in address not found \n");//GDPR
                    continue;
                }
                Matcher declarant2NameMatcher = declarant2NameInAddressPattern.matcher(zoneAddress);
                if (declarant2NameMatcher.find()) {
                    result.setDeclarant2Nom(fixLetter(declarant2NameMatcher.group(1)));
                } else {
                    log.info("\"Déclarant 2\" in address not found \n");//GDPR
                }

                String zoneRevenuPart = tesseract.doOCR(image, TaxIncomeZones.scale(zones.zoneRevenuPart, scale));
                Matcher partCountMatcher = partCountPattern.matcher(zoneRevenuPart);
                if (partCountMatcher.find()) {
                    result.setNombreDeParts(partCountMatcher.group(1).replaceAll("\\s", ""));
                } else {
                    log.warn("\"Nombre de parts\" not found \n" + zoneRevenuPart);
                    break;
                }
                Matcher incomeAmountMatcher = incomeAmountPattern.matcher(zoneRevenuPart);
                if (incomeAmountMatcher.find()) {
                    result.setRevenuFiscalDeReference(Integer.parseInt(fixNumber(incomeAmountMatcher.group(1))));
                    break;
                } else {
                    log.warn("\"Revenu fiscal de référence\" not found" + zoneRevenuPart);
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("Error during parsing", e);
        }
        return result;
    }

    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.TAX);
    }


    public record TaxIncomeZones(Rectangle zoneTitle, Rectangle zoneRef, Rectangle zoneAddress,
                                 Rectangle zoneRevenuPart) {
        public static Rectangle scale(Rectangle rectangle, double scale) {
            return new Rectangle((int) (rectangle.x * scale), (int) (rectangle.y * scale), (int) (rectangle.width * scale), (int) (rectangle.height * scale));
        }
    }
}
