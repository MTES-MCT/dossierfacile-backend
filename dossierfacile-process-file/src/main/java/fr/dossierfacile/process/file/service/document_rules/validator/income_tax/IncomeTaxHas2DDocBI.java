package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IncomeTaxHas2DDocBI extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_TAX_2D_DOC;
    }

    @Override
    protected boolean isValid(Document document) {

        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis barCodeFileAnalysis = dfFile.getFileAnalysis();
            if (barCodeFileAnalysis != null) {
                return true;
            }
        }
        return false;

    }
}