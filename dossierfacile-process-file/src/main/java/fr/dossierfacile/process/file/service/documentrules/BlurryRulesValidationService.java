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

    @Value("${blurry.laplacian.threshold:400}")
    private double laplacianThreshold;
    @Value("${blurry.sobel.threshold:30}")
    private double sobelThreshold;
    @Value("${blurry.fft.threshold:170}")
    private double fftThreshold;
    @Value("${blurry.dog.threshold:20}")
    private double dogThreshold;

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
        if (checkDogBlurryFile(blurryFileAnalysis)) {
            score++;
        }
        return score >= 3;
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

    private boolean checkDogBlurryFile(BlurryFileAnalysis blurryFileAnalysis) {
        var fftResult = blurryFileAnalysis.getBlurryResults().stream().filter(item -> item.algorithm() == BlurryAlgorithmType.DOG).findFirst();
        return fftResult
                .filter(blurryResult -> blurryResult.score() < dogThreshold)
                .isPresent();
    }

}