package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlurryRulesValidationService implements RulesValidationService {

    @Value("${blurry.laplacian.threshold}")
    private double laplacianThreshold;
    @Value("${blurry.sobel.threshold}")
    private double sobelThreshold;
    @Value("${blurry.fft.threshold}")
    private double fftThreshold;

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
        return report;
    }

    private boolean isFileBlurry(BlurryFileAnalysis blurryFileAnalysis) {
        var score = 0;
        if (checkLaplacianBlurryFile(blurryFileAnalysis)) {
            score++;
        }
        if (checkSobelBlurryFile(blurryFileAnalysis)) {
            score++;
        }
        if (checkFFTBlurryFile(blurryFileAnalysis)) {
            score++;
        }
        return score >= 2;
    }

    private boolean checkLaplacianBlurryFile(BlurryFileAnalysis blurryFileAnalysis) {
        var laplacianResult = blurryFileAnalysis.getBlurryResults().stream().filter(item -> item.algorithm() == BlurryAlgorithmType.LAPLACIEN).findFirst();
        return laplacianResult
                .filter(blurryResult -> blurryResult.score() < laplacianThreshold)
                .isPresent();
    }

    private boolean checkSobelBlurryFile(BlurryFileAnalysis blurryFileAnalysis) {
        var sobelResult = blurryFileAnalysis.getBlurryResults().stream().filter(item -> item.algorithm() == BlurryAlgorithmType.SOBEL).findFirst();
        return sobelResult
                .filter(blurryResult -> blurryResult.score() < sobelThreshold)
                .isPresent();
    }

    private boolean checkFFTBlurryFile(BlurryFileAnalysis blurryFileAnalysis) {
        var fftResult = blurryFileAnalysis.getBlurryResults().stream().filter(item -> item.algorithm() == BlurryAlgorithmType.FFT).findFirst();
        return fftResult
                .filter(blurryResult -> blurryResult.score() > fftThreshold)
                .isPresent();
    }

}