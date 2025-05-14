package fr.dossierfacile.process.file.service.processors.blurry;

import co.elastic.apm.api.CaptureSpan;
import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.common.repository.BlurryFileAnalysisRepository;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import fr.dossierfacile.process.file.service.processors.Processor;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.BlurryAlgorithm;
import fr.dossierfacile.process.file.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
// This processor use OpenCV to analyze if a file is blurry
public class BlurryProcessor implements Processor {

    @Value("${blurry.laplacian.threshold:400}")
    private int blurryLaplacianThreshold;

    @Value("${blurry.sobel.threshold:30}")
    private int blurrySobelThreshold;

    @Value("${blurry.fft.threshold:170}")
    private int blurryFftThreshold;

    @Value("${blurry.dog.threshold:20}")
    private int blurryDogThreshold;

    @Value("${opencv.lib.path}")
    private String opencvLibPath;

    public BlurryProcessor(
            List<BlurryAlgorithm> blurryAlgorithms,
            StorageFileLoaderService storageFileLoaderService,
            BlurryFileAnalysisRepository blurryFileAnalysisRepository,
            FileRepository fileRepository
    ) {
        this.blurryAlgorithms = blurryAlgorithms;
        this.storageFileLoaderService = storageFileLoaderService;
        this.blurryFileAnalysisRepository = blurryFileAnalysisRepository;
        this.fileRepository = fileRepository;
    }

    private static final int OPTIMIZED_FILE_DPI = 300;
    private final List<BlurryAlgorithm> blurryAlgorithms;
    private final StorageFileLoaderService storageFileLoaderService;
    private final BlurryFileAnalysisRepository blurryFileAnalysisRepository;
    private final FileRepository fileRepository;

    @PostConstruct
    public void initBlurryProcessor() {
        try {
            log.info("Loading OpenCV library");
            OpenCV.loadLocally();
        } catch (Exception e) {
            log.error("Error loading OpenCV library ", e);
        }
    }

    @CaptureSpan(value = "blurryAnalysis", type = "ANALYSIS", discardable = false)
    @Override
    public File process(File dfFile) {
        long start = System.currentTimeMillis();
        log.info("Starting blurry analysis of file");
        java.io.File file = storageFileLoaderService.getTemporaryFilePath(dfFile.getStorageFile());
        if (file == null) {
            log.error("File reading Error");
            return dfFile;
        }

        var blurryFileAnalysisBuilder = BlurryFileAnalysis.builder()
                .file(dfFile);

        try {
            var images = ImageUtils.getImagesFromFile(file);
            // We get a blurryResult for each algorithm and each image of a file (multiple images for pdf)
            List<List<BlurryResult>> listOfBlurryResults = Arrays.stream(images)
                    .map(image -> {
                        var img = getOpenCvOptimizedFile(image);
                        var result =  blurryAlgorithms.stream().map(algo -> algo.getBlurryResult(img)).toList();
                        img.release();
                        return result;
                    }).toList();

            blurryFileAnalysisBuilder.blurryResults(getWorstBlurryResult(listOfBlurryResults));
            blurryFileAnalysisBuilder.analysisStatus(BlurryFileAnalysisStatus.COMPLETED);
            var analysisResult = blurryFileAnalysisBuilder.build();
            blurryFileAnalysisRepository.save(analysisResult);

            dfFile.setBlurryFileAnalysis(analysisResult);
        } catch (IOException e) {
            log.error("Unable to get Images");
            var analysisResult = blurryFileAnalysisBuilder
                    .analysisStatus(BlurryFileAnalysisStatus.FAILED)
                    .build();
            blurryFileAnalysisRepository.save(analysisResult);
            dfFile.setBlurryFileAnalysis(analysisResult);
        }

        long end = System.currentTimeMillis();
        log.info("Finished blurry analysis of file in {} ms with status : {}",
                end - start,
                dfFile.getBlurryFileAnalysis() != null ? dfFile.getBlurryFileAnalysis().getAnalysisStatus() : null
        );

        fileRepository.save(dfFile);

        return dfFile;
    }

    private List<BlurryResult> getWorstBlurryResult(List<List<BlurryResult>> blurryResults) {
        List<BlurryResult> worstBlurryResult = null;
        double worstScore = Double.MIN_VALUE;

        for (var imageBlurryResult : blurryResults) {
            double score = calculateBlurryScore(imageBlurryResult);

            if (score > worstScore) {
                worstScore = score;
                worstBlurryResult = imageBlurryResult;
            }
        }

        return worstBlurryResult;
    }

    private double calculateBlurryScore(List<BlurryResult> imageBlurryResult) {
        return imageBlurryResult.stream()
                .mapToDouble(br -> switch (br.algorithm()) {
                    case LAPLACIEN -> br.score() / blurryLaplacianThreshold;
                    case SOBEL -> br.score() / blurrySobelThreshold;
                    case FFT -> blurryFftThreshold / br.score();
                    case DOG -> br.score() / blurryDogThreshold;
                })
                .sum();
    }

    @CaptureSpan(value = "getOpenCvOptimizedFile", type = "ANALYSIS", discardable = false)
    private Mat getOpenCvOptimizedFile(BufferedImage image) {
        Mat img = convert(image);

        final int DPI = OPTIMIZED_FILE_DPI;
        final int A4_WIDTH_PX = (int) (8.27 * DPI);
        final int A4_HEIGHT_PX = (int) (11.69 * DPI);

        int width = img.width();
        int height = img.height();

        // Calculate scale factor
        double widthScale = (double) A4_WIDTH_PX / width;
        double heightScale = (double) A4_HEIGHT_PX / height;
        double scale = Math.min(widthScale, heightScale);

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        Mat resizedImg = new Mat();
        Imgproc.resize(img, resizedImg, new Size(newWidth, newHeight));

        // Convert to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(resizedImg, gray, Imgproc.COLOR_BGR2GRAY);

        img.release();
        resizedImg.release();

        return gray;
    }

    @CaptureSpan(value = "convertImageForOpencv", type = "ANALYSIS", discardable = false)
    private Mat convert(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Mat mat;

        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat = new Mat(height, width, CvType.CV_8UC3);
            mat.put(0, 0, pixels);
        } else {
            BufferedImage convertedImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(image, 0, 0, null);
            byte[] pixels = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
            mat = new Mat(height, width, CvType.CV_8UC3);
            mat.put(0, 0, pixels);
        }

        return mat;
    }
}
