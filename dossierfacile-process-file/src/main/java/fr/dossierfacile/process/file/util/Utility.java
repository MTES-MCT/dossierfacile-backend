package fr.dossierfacile.process.file.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class Utility {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private static final String EXCEPTION_MESSAGE2 = "Exception while trying extract text to pdf";
    private final FileStorageService fileStorageService;

    public static String normalize(String s) {
        return Normalizer
                .normalize(s, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }

    public static Map<String, Integer> extractNumbersText(String s) {
        Map<String, Integer> numbers = new HashMap<>();
        if (!Strings.isEmpty(s)) {
            s = s.replaceAll("\\s", "");
            char[] chars = s.toCharArray();
            StringBuilder number = new StringBuilder();
            for (char c : chars) {
                if (Character.isDigit(c)) {
                    number.append(c);
                } else if (!number.toString().equals("")) {
                    if (numbers.get(number.toString()) != null) {
                        numbers.put(number.toString(), numbers.get(number.toString()) + 1);
                    } else {
                        numbers.put(number.toString(), 1);
                    }
                    number = new StringBuilder();
                }
            }
        }
        return numbers;
    }

    public static String extractFiscalNumber(String s) {
        Pattern p = Pattern.compile("[0-9]{2}\\s[0-9]{2}\\s[0-9]{3}\\s[0-9]{3}\\s[0-9]{3}");
        String result = "";
        Matcher m = p.matcher(s);
        if (m.find()) {
            String sMatch = m.group();
            result = sMatch.replaceAll("\\s", "");
        }
        return result;
    }

    public String extractInfoFromPDFFirstPage(File dfFile) {
        String pdfFileInText = null;
        try (InputStream fileInputStream = fileStorageService.download(dfFile)) {
            try (PDDocument document = PDDocument.load(fileInputStream)) {
                if (!document.isEncrypted()) {
                    PDFTextStripper reader = new PDFTextStripper();
                    reader.setAddMoreFormatting(true);
                    reader.setStartPage(1);
                    reader.setEndPage(1);
                    pdfFileInText = reader.getText(document);
                }
            }
        } catch (IOException e) {
            log.error(EXCEPTION_MESSAGE2, e);
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
        }
        return pdfFileInText;
    }

    public String extractQRCodeInfo(File dfFile) {
        String qrCodeInfo = "";
        try (InputStream inputStream = fileStorageService.download(dfFile)) {
            try (PDDocument document = PDDocument.load(inputStream)) {
                if (!document.isEncrypted()) {
                    PDFRenderer pdfRenderer = new PDFRenderer(document);

                    float dpi = (1058 / document.getPage(0).getMediaBox().getWidth()) * 300;
                    dpi = Math.min(600, dpi);
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, dpi, ImageType.ARGB);

                    BinaryBitmap binaryBitmap = new BinaryBitmap(
                            new HybridBinarizer(
                                    new BufferedImageLuminanceSource(bufferedImage)
                            ));
                    long time = System.currentTimeMillis();
                    Result result = new QRCodeReader().decode(binaryBitmap);
                    String decoded = result.getText();
                    log.info("DECODED QR : " + decoded + ", in " + (System.currentTimeMillis() - time) + "ms");
                    qrCodeInfo = decoded != null ? decoded : "";
                }
            } catch (IOException | NotFoundException | ChecksumException | FormatException e) {
                log.error(EXCEPTION_MESSAGE2, e);
                log.error(EXCEPTION + Sentry.captureException(e));
                log.error(e.getMessage(), e.getCause());
            }
        } catch (IOException e) {
            log.error("Unable to download file " + dfFile.getPath(), e);
            Sentry.captureMessage("Unable to download file " + dfFile.getPath());
        }

        return qrCodeInfo;
    }
}
