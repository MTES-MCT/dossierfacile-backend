package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
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
    private final Pattern paginationPattern = Pattern.compile("(\\d)[/|l1I](\\d)");

    @Override
    protected String getJsonModelFile() {
        return "/parsers/avisImpotsFeuillet.json";
    }

    @Override
    protected TaxIncomeLeaf getResultFromExtraction(Map<String, String> extractedText) {
        String numeroFiscal = extractedText.get("numeroFiscal").replaceAll("\\s", "");
        String pagination = extractedText.get("pagination").replaceAll("\\s", "");
        String anneeDesRevenus = extractedText.get("anneeDesRevenus").replaceAll("[^\\d]", "");
        Integer page = null, pageCount = null;

        Matcher pageInfo = paginationPattern.matcher(pagination);
        if (pageInfo.matches()) {
            try {
                page = Integer.parseInt(pageInfo.group(1));
                pageCount = Integer.parseInt(pageInfo.group(2));
            } catch (NumberFormatException pageCountException) {
                log.error("\"Information de feuillets\" issue", pageCountException);
            }
        }

        TaxIncomeLeaf result = TaxIncomeLeaf.builder().build();

        result.setAnneeDesRevenus(anneeDesRevenus);

        return TaxIncomeLeaf.builder().numeroFiscal(numeroFiscal).anneeDesRevenus(anneeDesRevenus).page(page).pageCount(pageCount).build();
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return file.getDocument().getDocumentCategory() == DocumentCategory.TAX && file.getDocument().getDocumentSubCategory() == MY_NAME;
    }
}
