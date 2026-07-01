package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;

// A property tax notice (taxe foncière) must contain at least 2 pages.
public class PropertyTaxLeafRule extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_PROPERTY_TAX_LEAF;
    }

    @Override
    protected boolean isValid(Document document) {
        var nbPages = document.getFiles().stream().map(File::getNumberOfPages).reduce(0, Integer::sum);
        return nbPages >= 2;
    }
}
