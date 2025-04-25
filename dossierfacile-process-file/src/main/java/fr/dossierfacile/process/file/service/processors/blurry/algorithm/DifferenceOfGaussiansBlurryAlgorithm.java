package fr.dossierfacile.process.file.service.processors.blurry.algorithm;

import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import lombok.AllArgsConstructor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

/*
 * DifferenceOfGaussiansBlurryAlgorithm evaluates image sharpness based on local contrast and texture strength.
 *
 * The algorithm applies the following steps:
 * 1. It applies two Gaussian blurs with different sigma values (σ1 < σ2) to the image.
 * 2. It computes the absolute difference between the two blurred images, emphasizing mid-frequency details.
 * 3. It calculates the variance (energy) of the resulting Difference of Gaussians (DoG) image.
 *
 * - A high energy value indicates strong texture and details (sharp image).
 * - A low energy value suggests a loss of local contrast, characteristic of blurry images.
 *
 * Important notes:
 * - The Gaussian blur kernel size is controlled by GAUSSIAN_SIZE.
 * - σ1 and σ2 are set to detect mid-range spatial frequencies (default: σ1=1.0, σ2=2.0).
 * - The result is encapsulated in a BlurryResult with the BlurryAlgorithmType.DOG.
 *
 * Usage context:
 * This method complements edge-based (Sobel, Laplacian) and frequency-based (FFT) approaches,
 * enhancing the robustness of the multi-algorithm blur detection system.
 */
@Service
@AllArgsConstructor
public class DifferenceOfGaussiansBlurryAlgorithm implements BlurryAlgorithm {

    private static final int GAUSSIAN_SIZE = 5;
    private static final double SIGMA1 = 1.0;
    private static final double SIGMA2 = 2.0;

    @Override
    public BlurryResult getBlurryResult(Mat img) {
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();
        Imgproc.GaussianBlur(img, blur1, new Size(GAUSSIAN_SIZE, GAUSSIAN_SIZE), SIGMA1);
        Imgproc.GaussianBlur(img, blur2, new Size(GAUSSIAN_SIZE, GAUSSIAN_SIZE), SIGMA2);

        Mat dog = new Mat();
        Core.absdiff(blur1, blur2, dog);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(dog, mean, stddev);

        double energy = Math.pow(stddev.get(0, 0)[0], 2);

        return new BlurryResult(BlurryAlgorithmType.DOG, energy);
    }
}
