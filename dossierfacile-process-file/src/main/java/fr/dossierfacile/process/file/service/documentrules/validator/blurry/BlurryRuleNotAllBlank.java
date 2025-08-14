package fr.dossierfacile.process.file.service.documentrules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;

import static fr.dossierfacile.process.file.service.documentrules.validator.blurry.BlurryRuleHelper.isBlank;

public class BlurryRuleNotAllBlank extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isValid(Document document) {
        boolean isNotAllBlank = false;
        for (var file : document.getFiles()) {
            BlurryFileAnalysis blurryFileAnalysis = file.getBlurryFileAnalysis();
            if (!isBlank(blurryFileAnalysis)) {
                isNotAllBlank = true;
            }
        }
        return isNotAllBlank;
    }

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
        return DocumentRule.R_BLURRY_FILE_BLANK;
    }
}
