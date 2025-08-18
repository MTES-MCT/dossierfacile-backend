package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import static fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderHelper.getGuaranteeProviderFile;

@Slf4j
public class IncomeTaxHas2DDoc extends AbstractDocumentRuleValidator {

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