package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Slf4j
public class IncomeTaxRuleCheckNMinus3 extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_TAX_N3;
    }


    @Override
    protected boolean isValid(Document document) {
        var providedYears = IncomeTaxHelper.getProvidedYears(document);
        if (providedYears.isEmpty()) {
            return false;
        }
        LocalDate now = LocalDate.now();
        Integer minYear = providedYears.stream().min(Integer::compare).orElse(null);
        int authorisedYear = now.minusMonths(9).minusDays(15).getYear();
        if (minYear == null) {
            return false;
        }
        return minYear >= (authorisedYear - 2);
    }

}