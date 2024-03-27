package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class TaxAssessmentParser extends AbstractImagesParser<TaxIncomeMainFile> implements FileParser<TaxIncomeMainFile> {
    private final Pattern declarant2NameInAddressPattern = Pattern.compile("OU (.+)\n");

    private final TaxIncomeLeafParser taxIncomeLeafParser;

    @Override
    protected String getJsonModelFile() {
        return "/parsers/avisImpotsModele1.json";
    }

    String fixNumber(String str) {
        return (str == null) ? null : str.trim().replaceAll("O", "0").replaceAll("[lI|]", "1").replaceAll("S", "5").replaceAll("[\\s\\n]", "");
    }

    String fixLetter(String str) {
        return (str == null) ? null : str.replaceAll("0", "O").replaceAll("5", "S").trim();
    }

    @Override
    protected TaxIncomeMainFile getResultFromExtraction(Map<String, String> extractedText) {

        String declarant1NumFiscal = fixNumber(extractedText.get("declarant1NumFiscal"));
        if (declarant1NumFiscal == null) {
            log.info("\"Déclarant 1 fiscal number\" not found \n - stop parsing");
            return null;
        }

        String zoneAddress = extractedText.get("nameAndAddressZone");
        String declarant1Name = fixLetter(zoneAddress.substring(0, zoneAddress.indexOf('\n')));
        if (declarant1Name == null) {
            log.info("\"Déclarant 1 name in addresse\" not found \n - stop parsing");
            return null;
        }
        String declarant2Name = null;
        Matcher declarant2NameMatcher = declarant2NameInAddressPattern.matcher(zoneAddress);
        if (declarant2NameMatcher.find()) {
            // TODO?
        }

        return TaxIncomeMainFile.builder()
                .anneeDesRevenus(Integer.parseInt(fixNumber(extractedText.get("incomeYear"))))
                .nombreDeParts(fixNumber(extractedText.get("nbParts")))
                .revenuFiscalDeReference(Integer.parseInt(fixNumber(extractedText.get("revenuFiscal"))))
                .referenceAvis(extractedText.get("referenceAvis"))
                .declarant1Nom(declarant1Name)
                .declarant2Nom(declarant2Name)
                .declarant1NumFiscal(declarant1NumFiscal)
                .declarant2NumFiscal(fixNumber(extractedText.get("declarant2NumFiscal")))
                .build();
    }

    @Override
    protected void enrichWithNextPages(BufferedImage[] images, TaxIncomeMainFile result) {
        List<TaxIncomeLeaf> leaves = new LinkedList<>();
        for (int i = 1; i < images.length; i++) {
            try {
                TaxIncomeLeaf leaf = taxIncomeLeafParser.parse(images[i]);
                leaves.add(leaf);
            } catch (Exception e) {
                log.warn("Page " + i + " cannot be parsed properly");
            }
        }
        result.setTaxIncomeLeaves(leaves);
    }

    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return file.getDocument().getDocumentCategory() == DocumentCategory.TAX
                && file.getDocument().getDocumentSubCategory() == MY_NAME;
    }

}
