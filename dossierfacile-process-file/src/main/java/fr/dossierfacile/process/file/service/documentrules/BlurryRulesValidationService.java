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
        var isBlurryDocument = false;
        for (var file : document.getFiles()) {
            if (isFileBlurry(file.getBlurryFileAnalysis())) {
                isBlurryDocument = true;
            }
        }
        if (isBlurryDocument) {
            report.getBrokenRules().add(DocumentBrokenRule.builder()
                    .rule(DocumentRule.R_BLURRY_FILE)
                    .message(DocumentRule.R_BLURRY_FILE.getDefaultMessage())
                    .build());
            report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
        }
        if (report.getBrokenRules().isEmpty()) {
            report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
        } else if (report.getBrokenRules().stream().anyMatch(r -> r.getRule().getLevel() == DocumentRule.Level.CRITICAL)) {
            report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
        } else {
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }

        return report;
    }

    private boolean isFileBlurry(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false;
        }
        if (blurryFileAnalysis.getAnalysisStatus() == BlurryFileAnalysisStatus.FAILED || blurryFileAnalysis.getBlurryResults() == null) {
            return false; // If the analysis failed, we cannot determine if the file is blurry
        } else {
            return blurryFileAnalysis.getBlurryResults().isBlurry();
        }
    }
}
