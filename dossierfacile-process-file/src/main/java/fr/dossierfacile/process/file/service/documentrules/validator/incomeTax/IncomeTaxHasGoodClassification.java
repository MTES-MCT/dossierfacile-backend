package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IncomeTaxHasGoodClassification extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_TAX_BAD_CLASSIFICATION;
    }

    @Override
    protected boolean isValid(Document document) {

        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis barCodeFileAnalysis = dfFile.getFileAnalysis();
            if (barCodeFileAnalysis == null) {
                break;
            }
            if (barCodeFileAnalysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT) {
                return true;
            }
        }
        return false;

    }
}