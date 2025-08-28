package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public class IncomeTaxRuleCheckNMinus1 extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_TAX_N1;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var brokenRule = checkMinus1(document);
        if (brokenRule != null) {
            log.info("Tax income is deprecated :{}", document.getId());
            return new RuleValidatorOutput(false, isBlocking(), brokenRule, RuleValidatorOutput.RuleLevel.FAILED);
        } else {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.PASSED);
        }

    }

    @Override
    protected boolean isValid(Document document) {
        throw new NotImplementedException();
    }

    private DocumentAnalysisRule checkMinus1(Document document) {
        List<Integer> providedYears = IncomeTaxHelper.getProvidedYears(document);
        if (providedYears.isEmpty()) {
            log.info("Tax income has no year :{}", document.getId());
            return DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_N1);
        }

        // Provide N-1 tax income / not more N-3
        int maxYear = providedYears.stream().max(Integer::compare).orElse(null);
        // try to check some rules
        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 9, 15))) {
            if (maxYear != (now.getYear() - 1) && maxYear != (now.getYear() - 2)) {
                return DocumentAnalysisRule.builder()
                        .rule(DocumentRule.R_TAX_N1)
                        .message("L'avis d'impots sur les revenus " + maxYear
                                + ", vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1)
                                + " ou " + (now.getYear() - 2))
                        .build();
            }
        } else if (maxYear != (now.getYear() - 1)) {
            return
                    DocumentAnalysisRule.builder()
                            .rule(DocumentRule.R_TAX_N1)
                            .message("L'avis d'impots sur les revenus " + maxYear
                                    + " n'est pas accepté, vous devez fournir un avis d'impots sur les revenus " + (now.getYear() - 1))
                            .build();
        }
        return null;
    }


}