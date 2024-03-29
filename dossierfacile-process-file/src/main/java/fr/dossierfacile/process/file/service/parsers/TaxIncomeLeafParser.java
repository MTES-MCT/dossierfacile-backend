package fr.dossierfacile.process.file.service.parsers;


import com.google.common.annotations.VisibleForTesting;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

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
@Order(3)
public class TaxIncomeLeafParser extends AbstractImagesParser<TaxIncomeLeaf> implements FileParser<TaxIncomeLeaf> {
    @VisibleForTesting
    protected final Pattern incomeYearPattern = Pattern.compile("revenus\\s*de\\s*(\\d{4})");
    @VisibleForTesting
    protected final Pattern fiscalNumberPattern = Pattern.compile("fiscal[:\\s]*([\\d \\s]+)");
    @VisibleForTesting
    protected final Pattern pageInfoPattern = Pattern.compile("Feuillet\\s*n.?[:\\s]+(\\d)[/\\s]+(\\d)");

    @Override
    protected String getJsonModelFile() {
        return "/parsers/avisImpotsFeuillet.json";
    }

    @Override
    protected TaxIncomeLeaf getResultFromExtraction(Map<String, String> extractedText) {

        TaxIncomeLeaf result = TaxIncomeLeaf.builder().build();
        try {

            String zoneHeaderLeft = extractedText.get("zoneHeaderLeft");
            Matcher matcher = fiscalNumberPattern.matcher(zoneHeaderLeft);
            if (matcher.find()) {
                result.setNumeroFiscal(matcher.group(1));
            } else {
                log.error("Numero Fiscal not found");
                return null;
            }

            String zoneHeaderRight = extractedText.get("zoneHeaderRight");
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
                    return null;
                }
            } catch (Exception e) {
                log.error("\"Information de feuillets\" error");
                return null;
            }
            Matcher yearMatcher = incomeYearPattern.matcher(zoneHeaderRight);
            if (yearMatcher.find()) {
                result.setAnneeDesRevenus(yearMatcher.group(1));
            } else {
                log.warn("\"Année des revenus\" not found" + zoneHeaderRight);
                return null;
            }

        } catch (Exception e) {
            log.error("Error during parsing", e);
            return null;
        }
        return result;
    }

    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return file.getDocument().getDocumentCategory() == DocumentCategory.TAX
                && file.getDocument().getDocumentSubCategory() == MY_NAME;
    }
}
