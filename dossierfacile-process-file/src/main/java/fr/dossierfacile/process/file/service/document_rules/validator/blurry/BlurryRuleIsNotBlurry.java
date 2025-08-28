package fr.dossierfacile.process.file.service.document_rules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import org.springframework.lang.Nullable;

import static fr.dossierfacile.process.file.service.document_rules.validator.blurry.BlurryRuleHelper.isBlank;

public class BlurryRuleIsNotBlurry extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isValid(Document document) {
        for (var file : document.getFiles()) {
            BlurryFileAnalysis blurryFileAnalysis = file.getBlurryFileAnalysis();
            if (isBlank(blurryFileAnalysis)) {
                continue;
            }
            if (isFileBlurry(blurryFileAnalysis)) {
                return false;
            }
        }
        return true;
    }

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
        return DocumentRule.R_BLURRY_FILE;
    }

    private boolean isFileBlurry(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false;
        }
        if (blurryFileAnalysis.getBlurryResults().isReadable()) {
            return blurryFileAnalysis.getBlurryResults().isBlurry();
        } else {
            return true;
        }
    }
}
