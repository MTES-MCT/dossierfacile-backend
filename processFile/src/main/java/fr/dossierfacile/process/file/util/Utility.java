package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.service.interfaces.OvhService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class Utility {

    private static final String EXCEPTION_MESSAGE2 = "Exception white trying extract text to pdf";
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

    public String extractTextPDFFirstPage(String pathFile) {
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
                Sentry.capture(e);
            }
        }
        return pdfFileInText;
    }
}
