package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.SCHOLARSHIP;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class ScholarshipCROUSParser extends AbstractPDFParser<ScholarshipFile> implements FileParser<ScholarshipFile> {

    private static final Pattern notificationPattern = Pattern.compile("NOTIFICATION [^\\d]{0,20}(\\d{4})[^\\d]{0,4}(\\d{4})");

    @Override
    protected String getJsonModelFile() {
        return "/parsers/bourseCROUS.json";
    }

    @Override
    protected AbstractPDFParser<ScholarshipFile> getPageParser(int i) {
        // Le bulletin de salaire peut etre en plusieurs page
        // Si c'est le cas, il est possible que la somme totale ne soit indiqué qu'à la page 2
        // Le template est exactement le meme
        // Pour simplifier, on utilise simplement le même parser qui ecrasera les données
        if (i == 1) {
            return new ScholarshipCROUSPage2Parser();
        }
        return super.getPageParser(i);
    }

    @Override
    protected ScholarshipFile getResultFromExtraction(PDFTextStripperByArea stripper, ScholarshipFile previousResult) {

        Matcher notificationMatcher = notificationPattern.matcher(stripper.getTextForRegion("notification").trim());
        if (!notificationMatcher.matches()) {
            log.error("Unable to extract year from notification");
            return null;
        }
        Integer startYear = Integer.parseInt(notificationMatcher.group(1));
        Integer endYear = Integer.parseInt(notificationMatcher.group(2));

        return ScholarshipFile.builder()
                .lastName(stripper.getTextForRegion("lastname").trim())
                .firstName(stripper.getTextForRegion("firstname").trim())
                .notificationReference("notificationReference")
                .startYear(startYear)
                .endYear(endYear)
                .build();
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.FINANCIAL
                && file.getDocument().getDocumentSubCategory() == SCHOLARSHIP
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}