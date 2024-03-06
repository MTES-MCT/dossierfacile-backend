package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.TENANT;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class RentalReceiptParser extends AbstractPDFParser<RentalReceiptFile> implements FileParser<RentalReceiptFile> {

    private static final Pattern periodPattern = Pattern.compile("du (\\d{2}/\\d{2}/\\d{4}) au (\\d{2}/\\d{2}/\\d{4}).*");

    @Override
    protected String getJsonModelFile() {
        return "/parsers/rentalReceiptStd1.json";
    }

    @Override
    protected RentalReceiptFile getResultFromExtraction(PDFTextStripperByArea stripper, int pageNumber, RentalReceiptFile previousResult){
        if (!"Adresse du bien loué".equals(stripper.getTextForRegion("rentAddressLabel").trim())) {
            // format is not the same
            return null;
        }
        Pattern pattern = Pattern.compile(".*QUITTANCE DE LOYER.*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stripper.getTextForRegion("rentReceiptLabel"));
        if (!matcher.matches()) {
            // format is not the same
            return null;
        }

        Matcher periodMatcher = periodPattern.matcher(stripper.getTextForRegion("period").trim());
        if (!periodMatcher.matches()) {
            // format is not the same
            log.warn("Period not found");
            return null;
        }

        YearMonth period = null;
        try {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate endDate = LocalDate.parse(periodMatcher.group(2), dateFormat);
            period = YearMonth.from(endDate);
        } catch (Exception e) {
            log.warn("Period wrong format");
            return null;
        }
        Double amount = null;
        try {
            // Caution this std rent receipt have specific double encoding...
            amount = Double.parseDouble(stripper.getTextForRegion("amount").replaceAll("[ .]", "").replace(",", "."));
        } catch (Exception e) {
            log.warn("Amount wrong format");
            return null;
        }

        String fullAddress = stripper.getTextForRegion("tenantNameAndAddress"); // nom en 1ere ligne
        String fullName = fullAddress.substring(0, fullAddress.indexOf('\n')).trim();

        return RentalReceiptFile.builder()
                .tenantFullName(fullName)
                .period(period)
                .amount(amount)
                .build();
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.RESIDENCY
                && file.getDocument().getDocumentSubCategory() == TENANT
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}
