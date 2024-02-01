package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PublicPayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicPayslipParser implements FileParser<PublicPayslipFile> {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("MMMM yyyy").toFormatter(Locale.FRENCH);

    @Override
    public PublicPayslipFile parse(File file) {
        PublicPayslipFile result = PublicPayslipFile.builder().build();
        // file is a pdf
        try (PDDocument document = Loader.loadPDF(file)) {
            PDDocumentInformation info = document.getDocumentInformation();

            if (info == null || info.getTitle() == null || !info.getTitle().equalsIgnoreCase("PAY18E")) {
                log.info("This document is not in PAY18E format");
                return null;
            }

            PDPage page = document.getPage(0);
            double scale = page.getMediaBox().getWidth() / 595;

            Rectangle titleRect = new Rectangle((int) (273 * scale), (int) (10 * scale), (int) (134 * scale), (int) (8 * scale));
            Rectangle monthRect = new Rectangle((int) (315 * scale), (int) (22 * scale), (int) (100 * scale), (int) (11 * scale));
            Rectangle netTaxableIncomeRect = new Rectangle((int) (125 * scale), (int) (711 * scale), (int) (80 * scale), (int) (13 * scale));
            Rectangle cumulativeNetIncomeRect = new Rectangle((int) (25 * scale), (int) (711 * scale), (int) (84 * scale), (int) (13 * scale));
            Rectangle fullnameRect = new Rectangle((int) (215 * scale), (int) (680 * scale), (int) (266 * scale), (int) (35 * scale));

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.addRegion("title", titleRect);
            stripper.addRegion("month", monthRect);
            stripper.addRegion("netTaxableIncome", netTaxableIncomeRect);
            stripper.addRegion("cumulativeNetIncome", cumulativeNetIncomeRect);
            stripper.addRegion("fullname", fullnameRect);

            stripper.extractRegions(page);

            String title = stripper.getTextForRegion("title");
            if ("BULLETIN DE PAYE".equalsIgnoreCase(title)) {
                log.info("This document is not a public payslip");
                return null;
            }
            String month = stripper.getTextForRegion("month").trim();
            String netTaxableIncome = stripper.getTextForRegion("netTaxableIncome").trim();
            String cumulativeNetIncome = stripper.getTextForRegion("cumulativeNetIncome").trim();
            String fullname = stripper.getTextForRegion("fullname").replaceAll("\n.*", "").trim();

            result.setNetTaxableIncome(Double.parseDouble(netTaxableIncome.replace(" ", "").replace(',', '.')));
            result.setCumulativeNetTaxableIncome(Double.parseDouble(cumulativeNetIncome.replace(" ", "").replace(',', '.')));
            result.setFullname(fullname);

            result.setMonth(YearMonth.parse(month, YEAR_MONTH_FORMATTER));

            return result;
        } catch (Exception e) {
            log.error("Unable to parse");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.FINANCIAL
                && file.getDocument().getDocumentSubCategory() == SALARY
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}