package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Parsing POC
 */
@Slf4j
@RequiredArgsConstructor
public class ScholarshipCROUSPage2Parser extends AbstractPDFParser<ScholarshipFile> implements FileParser<ScholarshipFile> {
    private static final Pattern amountPattern = Pattern.compile("(\\d{1,5}),\\d{2}€.");

    @Override
    protected String getJsonModelFile() {
        return "/parsers/bourseCROUSPage2.json";
    }

    @Override
    protected ScholarshipFile getResultFromExtraction(PDFTextStripperByArea stripper, ScholarshipFile previousResult) {
        if (previousResult == null) {
            previousResult = ScholarshipFile.builder()
                    .notificationReference(stripper.getTextForRegion("notificationReference").trim())
                    .build();
        }

        Matcher amountMatcher = amountPattern.matcher(stripper.getTextForRegion("amount").trim());
        if (!amountMatcher.matches()) {
            log.error("Unable to extract amount");
            return null;
        }
        previousResult.setAnnualAmount(Integer.parseInt(amountMatcher.group(1)));

        return previousResult;
    }

    @Override
    public boolean shouldTryToApply(File file) {
        // Currently only run as pageParser
        return false;
    }
}
