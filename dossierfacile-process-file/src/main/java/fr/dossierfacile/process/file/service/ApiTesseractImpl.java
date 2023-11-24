package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.utils.FileUtility;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.PdfUtilities;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;


@Service
@Slf4j
public class ApiTesseractImpl implements ApiTesseract {

    @Override
    public String extractText(File file) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setLanguage("fra+eng");
            tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            tesseract.setVariable("user_defined_dpi", "300");

            BufferedImage image = null;
            if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
                BufferedImage[] images = FileUtility.convertPdfToImage(file);
                if (images == null || images.length == 0) {
                    throw new IllegalStateException("pdf file cannot be convert to images");
                }
                image = images[0];
            }

            return tesseract.doOCR(image);
        } catch (Exception e) {
            log.error("Error during tesseract text extraction", e);
            Sentry.captureException(e);
        }
        return "";
    }

}
