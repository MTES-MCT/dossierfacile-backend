package fr.dossierfacile.process.file.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import fr.dossierfacile.common.service.interfaces.OvhService;
import io.sentry.Sentry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Utility {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private static final String EXCEPTION_MESSAGE2 = "Exception while trying extract text to pdf";
    private final OvhService ovhService;

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

    public static String extractReferenceNumber(String s) {
        Pattern p = Pattern.compile("[A-Z0-9]{2}\\s[A-Z0-9]{2}\\s[A-Z0-9]{7}\\s[0-9]{2}");
        Matcher m = p.matcher(s);
        String result = "";
        while (m.find()) {
            String sMatch = m.group();
            if (countDigits(sMatch) > countDigits(result)) {
                result = sMatch.replaceAll("\\s", "");
            }
        }
        return result;
    }

    private static int countDigits(String s) {
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    public String extractInfoFromPDFFirstPage(String pathFile) {
        String pdfFileInText = null;
        SwiftObject swiftObject = ovhService.get(pathFile);
        if (swiftObject != null) {
            try (PDDocument document = PDDocument.load(swiftObject.download().getInputStream())) {
                if (!document.isEncrypted()) {
                    PDFTextStripper reader = new PDFTextStripper();
                    reader.setAddMoreFormatting(true);
                    reader.setStartPage(1);
                    reader.setEndPage(1);
                    pdfFileInText = reader.getText(document);
                }
            } catch (IOException e) {
                log.error(EXCEPTION_MESSAGE2, e);
                log.error(EXCEPTION + Sentry.captureException(e));
                log.error(e.getMessage(), e.getCause());
            }
        }
        return pdfFileInText;
    }

    public String extractQRCodeInfo(String pathFile) {
        String qrCodeInfo = "";
        SwiftObject swiftObject = ovhService.get(pathFile);
        if (swiftObject != null) {
            try (PDDocument document = PDDocument.load(swiftObject.download().getInputStream())) {
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
        }

        return qrCodeInfo;
    }
}
