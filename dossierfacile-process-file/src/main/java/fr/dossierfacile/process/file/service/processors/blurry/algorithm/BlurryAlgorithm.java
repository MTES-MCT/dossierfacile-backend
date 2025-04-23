package fr.dossierfacile.process.file.service.processors.blurry.algorithm;

import fr.dossierfacile.common.entity.ocr.BlurryResult;
import org.opencv.core.Mat;

public interface BlurryAlgorithm {
    BlurryResult isBlurry(Mat img);
}
