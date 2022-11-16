package fr.dossierfacile.process.file.service;

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

            if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
                File[] files = PdfUtilities.convertPdf2Png(file);
                if (files == null || files.length == 0) {
                    throw new IllegalStateException("pdf file cannot be convert to images");
                }
                file = files[0];
            }

            BufferedImage image = ImageIO.read(file);
            return tesseract.doOCR(image);
        } catch (Exception e) {
            log.error("Error during tesseract text extraction", e);
            Sentry.captureException(e);
        }
        return "";
    }

}
