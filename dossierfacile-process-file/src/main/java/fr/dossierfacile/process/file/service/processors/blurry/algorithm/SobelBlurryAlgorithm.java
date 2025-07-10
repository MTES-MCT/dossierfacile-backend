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

/*
 * SobelBlurryAlgorithm estimates the sharpness of an image based on the intensity of edges detected.
 *
 * The algorithm applies the Sobel operator separately in the X and Y directions
 * to compute gradients, which highlight transitions in pixel intensity (edges).
 * It then calculates the magnitude of the gradients and computes the mean magnitude across the entire image.
 *
 * - A high mean magnitude indicates strong and numerous edges (sharp image).
 * - A low mean magnitude suggests weak or few edges (potentially blurry image).
 *
 * This mean score can be compared against a predefined threshold to determine if
 * the document should be classified as blurry.
 *
 * Note:
 * - Gradients are computed using 64-bit floating point precision (CvType.CV_64F).
 * - The final result is encapsulated into a BlurryResult with type BlurryAlgorithmType.SOBEL.
 *
 * Usage context:
 * Sobel analysis is combined with other algorithms (Laplacian, FFT, DoG) within a voting system
 * to enhance the overall reliability of document blur detection.
 */
@Service
@AllArgsConstructor
public class SobelBlurryAlgorithm implements BlurryAlgorithm {

    @Override
    public BlurryResult getBlurryResult(Mat img) {
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
