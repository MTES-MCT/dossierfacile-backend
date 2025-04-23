package fr.dossierfacile.process.file.service.processors.blurry.algorithm;

import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import lombok.AllArgsConstructor;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SobelBlurryAlgorithm implements BlurryAlgorithm {

    @Override
    public BlurryResult isBlurry(Mat img) {
        Mat sobelX = new Mat();
        Mat sobelY = new Mat();

        Imgproc.Sobel(img, sobelX, CvType.CV_64F, 1, 0, 3);
        Imgproc.Sobel(img, sobelY, CvType.CV_64F, 0, 1, 3);

        Mat magnitude = new Mat();
        Core.magnitude(sobelX, sobelY, magnitude);

        Scalar mean = Core.mean(magnitude);
        double meanVal = mean.val[0];

        return new BlurryResult(BlurryAlgorithmType.SOBEL, meanVal);
    }
}
