package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;

public class TaxLeafRuleB extends BaseTaxRule {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_TAX_LEAF;
    }

    @Override
    protected boolean isValid(Document document) {

        var nbPages = document.getFiles().stream().map(File::getNumberOfPages).reduce(0, Integer::sum);

        return nbPages >= 2;
    }
}
