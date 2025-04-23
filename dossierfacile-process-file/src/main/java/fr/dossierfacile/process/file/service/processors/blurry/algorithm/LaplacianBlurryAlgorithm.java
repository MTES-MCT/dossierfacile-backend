package fr.dossierfacile.process.file.service.processors.blurry.algorithm;

import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import lombok.AllArgsConstructor;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LaplacianBlurryAlgorithm implements BlurryAlgorithm {

    @Override
    public BlurryResult isBlurry(Mat img) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(img, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stdDev);

        double variance = Math.pow(stdDev.get(0, 0)[0], 2);
        return new BlurryResult(BlurryAlgorithmType.LAPLACIEN, variance);
    }
}
