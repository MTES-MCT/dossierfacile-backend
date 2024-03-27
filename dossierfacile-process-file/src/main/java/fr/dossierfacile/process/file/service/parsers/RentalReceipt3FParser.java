package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.TENANT;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(4)
public class RentalReceipt3FParser extends AbstractImagesParser<RentalReceiptFile> implements FileParser<RentalReceiptFile> {

    private static final Pattern periodPattern = Pattern.compile("Période du (\\d{2}/\\d{2}/\\d{4}) au (\\d{2}/\\d{2}/\\d{4}).*");
    private static final Pattern amountBalancePattern = Pattern.compile("Votre Solde au \\d{2}/\\d{2}/\\d{4}\s+(\\d[ \\d,.]+)€.*");
    private static final Pattern amountInvoicePattern = Pattern.compile("Facture du \\d{2}/\\d{2}/\\d{4} au \\d{2}/\\d{2}/\\d{4}\s+(\\d[ \\d,.]+)€.*");
    @Override
    protected String getJsonModelFile() {
        return "/parsers/rentalReceipt3F.json";
    }

    @Override
    protected RentalReceiptFile getResultFromExtraction(Map<String, String> extractedText) {
        Matcher periodMatcher = periodPattern.matcher(extractedText.get("periodNotice").trim());
        if (!periodMatcher.matches()) {
            // format is not the same
            log.warn("Period not found");
            return null;
        }

        YearMonth period = null;
        try {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate endDate = LocalDate.parse(periodMatcher.group(2), dateFormat);
            period = YearMonth.from(endDate).minusMonths(1);// il s'agit d'un avis d'échéance
        } catch (Exception e) {
            log.warn("Period wrong format");
            return null;
        }

        Matcher balanceMatcher = amountBalancePattern.matcher(extractedText.get("previousAmountBalance").trim());
        if (!balanceMatcher.matches()) {
            // format is not the same
            log.warn("Balance not found");
            return null;
        }
        try {
            // Caution this std rent receipt have specific double encoding...
            Double balance = Double.parseDouble(balanceMatcher.group(1).replaceAll("[ .]", "").replace(",", "."));
            if (balance > 50 ){
                // si le solde n'est pas env. =0, ce n'est pas une quittance mais un simple avis d'écheance
                return null;
            }
        } catch (Exception e) {
            log.warn("Balance wrong format");
            return null;
        }

        Matcher amountMatcher = amountInvoicePattern.matcher(extractedText.get("amount").trim());
        if (!amountMatcher.matches()) {
            // format is not the same
            log.warn("Amount not found");
            return null;
        }
        Double amount = null;
        try {
            // Caution this std rent receipt have specific double encoding...
            amount = Double.parseDouble(amountMatcher.group(1).replaceAll("[ .]", "").replace(",", "."));
        } catch (Exception e) {
            log.warn("Amount wrong format");
            return null;
        }

        String fullAddress = extractedText.get("tenantNameAndAddress"); // nom en 1ere ligne
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
