package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlurryRulesValidationService implements RulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getFiles().stream().allMatch(file -> file.getBlurryFileAnalysis() != null)
                && !CollectionUtils.isEmpty(document.getFiles());
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {
        boolean isBlurryDocument = false;
        boolean isAllBlank = true;
        for (var file : document.getFiles()) {
            BlurryFileAnalysis blurryFileAnalysis = file.getBlurryFileAnalysis();
            if (!hasBeenAnalysed(blurryFileAnalysis)) {
                log.warn("Blurry analysis for file {} has failed, skipping blurry rules validation", file.getId());
                report.addDocumentInconclusiveRule(DocumentAnalysisRule.documentInconclusiveRuleFrom(DocumentRule.R_BLURRY_FILE));
                break;
            } else {
                if (!isBlank(blurryFileAnalysis)) {
                    isAllBlank = false;
                }
                isBlurryDocument = isFileBlurry(blurryFileAnalysis);
            }
        }
        if (isAllBlank) {
            log.warn("Blurry analysis for document {} is blank, skipping blurry rules validation", document.getId());
            report.addDocumentInconclusiveRule(DocumentAnalysisRule.documentInconclusiveRuleFrom(DocumentRule.R_BLURRY_FILE));
            return report;
        }
        if (isBlurryDocument) {
            report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_BLURRY_FILE));
        } else {
            report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_BLURRY_FILE));
        }
        return report;
    }

    private boolean hasBeenAnalysed(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false;
        }
        return blurryFileAnalysis.getAnalysisStatus() != BlurryFileAnalysisStatus.FAILED;
    }

    private boolean isBlank(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false; // If the analysis is null, we consider it as blank
        }
        return blurryFileAnalysis.getBlurryResults().isBlank();
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
