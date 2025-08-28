package fr.dossierfacile.process.file.service.document_rules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

@Slf4j
public class BlurryRuleHasBeenAnalysedBI extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isValid(Document document) {
        for (var file : document.getFiles()) {
            BlurryFileAnalysis blurryFileAnalysis = file.getBlurryFileAnalysis();
            if (!hasBeenAnalysed(blurryFileAnalysis)) {
                log.warn("Blurry analysis for file {} has failed, skipping blurry rules validation", file.getId());
                return false;
            }
        }
        return true;
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
        return DocumentRule.R_BLURRY_FILE_ANALYSED;
    }

    private boolean hasBeenAnalysed(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false;
        }
        return blurryFileAnalysis.getAnalysisStatus() != BlurryFileAnalysisStatus.FAILED;
    }
}
