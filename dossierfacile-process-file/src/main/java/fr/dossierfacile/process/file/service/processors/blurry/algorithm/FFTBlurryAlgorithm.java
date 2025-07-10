package fr.dossierfacile.process.file.service.processors.blurry.algorithm;

import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import lombok.AllArgsConstructor;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * FFTBlurryAlgorithm assesses the sharpness of an image using frequency domain analysis.
 *
 * The algorithm performs the following steps:
 * 1. It applies a Fourier Transform (FFT) to the image to compute its frequency spectrum.
 * 2. It shifts the low-frequency components to the center of the spectrum (fftShift).
 * 3. It analyzes a circular region around the center to calculate the mean energy.
 *
 * - A low mean energy in the center suggests a rich presence of high frequencies (sharp image).
 * - A high mean energy in the center indicates that most of the energy is concentrated in low frequencies (potentially blurry image).
 *
 * Important notes:
 * - The FFT is computed with optimal padding for efficient processing.
 * - The magnitude spectrum is log-scaled and normalized for better numerical stability.
 * - The radius of the center analysis area is defined by DEFAULT_RADIUS (typically set to 30 pixels).
 *
 * The final mean value is returned in a BlurryResult with the BlurryAlgorithmType.FFT.
 *
 * Usage context:
 * This algorithm complements spatial-based methods (e.g., Sobel, Laplacian) within a voting system
 * to provide a robust multi-perspective assessment of document sharpness.
 */
@Service
@AllArgsConstructor
public class FFTBlurryAlgorithm implements BlurryAlgorithm {

    private static final int DEFAULT_RADIUS = 30;

    @Override
    public BlurryResult getBlurryResult(Mat img) {
        Mat padded = optimalPadding(img);
        // FFT
        Mat complexImage = new Mat();
        padded.convertTo(padded, CvType.CV_32F);
        Core.dft(padded, complexImage, Core.DFT_COMPLEX_OUTPUT);

        // Magnitude spectrum
        Mat magnitude = magnitudeSpectrum(complexImage);

        // Décalage (fftshift)
        Mat shiftedMagnitude = fftShift(magnitude);

        // Analyse autour du centre
        double fftMean = analyzeCenterRegion(shiftedMagnitude);

        return new BlurryResult(BlurryAlgorithmType.FFT, fftMean);
    }

    private Mat optimalPadding(Mat image) {
        int rows = Core.getOptimalDFTSize(image.rows());
        int cols = Core.getOptimalDFTSize(image.cols());
        Mat padded = new Mat();
        Core.copyMakeBorder(image, padded, 0, rows - image.rows(), 0, cols - image.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
        return padded;
    }

    private Mat magnitudeSpectrum(Mat complexImage) {
        List<Mat> planes = new ArrayList<>();
        Core.split(complexImage, planes);
        Mat magnitude = new Mat();
        Core.magnitude(planes.get(0), planes.get(1), magnitude);

        Core.add(Mat.ones(magnitude.size(), CvType.CV_32F), magnitude, magnitude);
        Core.log(magnitude, magnitude);

        Core.normalize(magnitude, magnitude, 0, 255, Core.NORM_MINMAX);
        magnitude.convertTo(magnitude, CvType.CV_8U);

        return magnitude;
    }

    private Mat fftShift(Mat input) {
        Mat output = input.clone();
        int cx = output.cols() / 2;
        int cy = output.rows() / 2;

        Mat q0 = new Mat(output, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(output, new Rect(cx, 0, cx, cy));
        Mat q2 = new Mat(output, new Rect(0, cy, cx, cy));
        Mat q3 = new Mat(output, new Rect(cx, cy, cx, cy));

        Mat tmp = new Mat();
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);

        q1.copyTo(tmp);
        q2.copyTo(q1);
        tmp.copyTo(q2);

        return output;
    }

    private double analyzeCenterRegion(Mat magnitude) {
        double x = (double) magnitude.cols() / 2;
        double y = (double) magnitude.rows() / 2;
        Point center = new Point(x, y);
        Mat mask = Mat.zeros(magnitude.size(), CvType.CV_8U);
        Imgproc.circle(mask, center, DEFAULT_RADIUS, new Scalar(255), -1);

        Scalar meanVal = Core.mean(magnitude, mask);

        return meanVal.val[0];
    }
}
