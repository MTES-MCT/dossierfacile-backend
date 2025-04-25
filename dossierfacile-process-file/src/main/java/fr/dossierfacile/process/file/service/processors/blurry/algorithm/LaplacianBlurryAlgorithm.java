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

/*
 * LaplacianBlurryAlgorithm is a simple and efficient method to assess the sharpness of an image.
 *
 * The algorithm applies a Laplacian filter to highlight areas of rapid intensity change,
 * such as edges and fine details. It then calculates the variance of the Laplacian response.
 *
 * - A high variance indicates that many edges and details are present (sharp image).
 * - A low variance suggests few edges and smooth areas (potentially blurry image).
 *
 * This variance score can then be compared against a predefined threshold to classify
 * whether the document is considered blurry or not.
 *
 * Note:
 * - The Laplacian is computed using 64-bit floating point precision (CvType.CV_64F).
 * - The result is wrapped into a BlurryResult object with the type BlurryAlgorithmType.LAPLACIEN.
 *
 * Usage context:
 * This algorithm is part of a multi-algorithm voting system designed to improve document
 * blur detection reliability in dossier uploads.
 */
@Service
@AllArgsConstructor
public class LaplacianBlurryAlgorithm implements BlurryAlgorithm {

    @Override
    public BlurryResult getBlurryResult(Mat img) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(img, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stdDev);

        double variance = Math.pow(stdDev.get(0, 0)[0], 2);
        return new BlurryResult(BlurryAlgorithmType.LAPLACIEN, variance);
    }
}
