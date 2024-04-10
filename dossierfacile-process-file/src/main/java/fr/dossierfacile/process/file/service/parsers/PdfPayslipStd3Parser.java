package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;

@Service
@Slf4j
@RequiredArgsConstructor
@Order(2)
public class PdfPayslipStd3Parser extends AbstractPDFParser<PayslipFile> implements FileParser<PayslipFile> {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM yyyy").toFormatter(Locale.FRENCH);
    private static final Pattern patternPeriodLastDay = Pattern.compile("\\s*\\d{1,2}\\s*(?<month>\\w+\\s*\\d{4})\\s*\n*");

    @Override
    protected PayslipFile getResultFromExtraction(PDFTextStripperByArea stripper, PayslipFile result) {

        String periodStr = stripper.getTextForRegion("periodLastDay").trim();
        Matcher matcherPeriod = patternPeriodLastDay.matcher(periodStr);
        if (!matcherPeriod.matches()) {
            log.warn("Period not found");
            return null;
        }
        YearMonth month = YearMonth.parse(matcherPeriod.group("month").trim(), YEAR_MONTH_FORMATTER);

        String fullAddress = stripper.getTextForRegion("address"); // nom en 1ere ligne
        String fullName = fullAddress.substring(0, fullAddress.indexOf('\n')).trim();

        Double netIncome = null;
        try {
            netIncome = Double.parseDouble(stripper.getTextForRegion("netIncome").replaceAll(" ", "").replace(",", "."));
        } catch (Exception e) {
            log.warn("netIncome wrong format");
        }

        try {
            LocalDate entryDate = LocalDate.parse(stripper.getTextForRegion("entryDate").trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            log.warn("entryDate wrong format");
        }

        return PayslipFile.builder()
                .fullname(fullName)
                .month(month)
                .netTaxableIncome(netIncome)
                .build();
    }

    @Override
    protected String getJsonModelFile() {
        return "/parsers/payslip3.json";
    }

    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.FINANCIAL
                && file.getDocument().getDocumentSubCategory() == SALARY
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}