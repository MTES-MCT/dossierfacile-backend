package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocIdEnum;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocReader;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDoc;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    @Value("${directory.ocr.path:/app/ocr}")
    private String ocrDirectoryPath;
    private static final char ASCII_GROUP_SEPARATOR = (char)29;
    private static final char ASCII_UNIT_SEPARATOR = (char)31;

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
                    if (numbers.get(number.toString()) == null && number.length() <= 9) {
                        numbers.put(number.toString(), Integer.valueOf(number.toString()));
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

    public java.io.File getTemporaryFile(File dfFile) {
        try (InputStream fileInputStream = fileStorageService.download(dfFile.getStorageFile())) {
            java.io.File myFilesDirectory = new java.io.File(ocrDirectoryPath);
            try {
                if (!myFilesDirectory.exists()) {
                    if (!myFilesDirectory.mkdir()) {
                        log.error("Unable create directory 'mkdir returns false':" + ocrDirectoryPath);
                        throw new IOException("Unable to create directory " + ocrDirectoryPath);
                    }
                }
            } catch (SecurityException e) {
                log.error("Security : Unable read/write directory " + ocrDirectoryPath + ":" + e.getCause(), e);
                throw e;
            }

            java.io.File tempFile = java.io.File.createTempFile("tmp", dfFile.getStorageFile().getPath(), myFilesDirectory );

            Files.copy(fileInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (Exception e) {
            log.error("Unable to write temporary File on instance", e);
            Sentry.captureException(e);
        }
        return null;
    }

    public String extractInfoFromPDFFirstPage(File dfFile) {
        String pdfFileInText = null;
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(dfFile, fileStorageService)) {
            pdfFileInText = inMemoryPdfFile.getContentAsString();
        } catch (IOException e) {
            log.error(EXCEPTION_MESSAGE2, e);
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
        }
        return pdfFileInText;
    }

    public String extractTax2DDoc(StorageFile file) {
        try (InputStream inputStream = fileStorageService.download(file)) {
            try (PDDocument document = PDDocument.load(inputStream)) {
                return TwoDDocReader.find2DDocOn(document)
                        .map(TwoDDocRawContent::rawContent)
                        .orElse("");
            } catch (IOException e) {
                log.warn(EXCEPTION_MESSAGE2, e);
                log.error(EXCEPTION + Sentry.captureException(e));
                log.error(e.getMessage(), e.getCause());
            }
        } catch (IOException e) {
            log.error("Unable to download file " + file.getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getPath());
        }

        return "";
    }

    @Deprecated
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

     static int countDigits(String s) {
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }
        return count;
    }


    public TwoDDoc parseTwoDDoc(String twoDDocContent) {
        TwoDDoc twoDDoc = new TwoDDoc();

        if (twoDDocContent == null || twoDDocContent.length() < 26) {
            return twoDDoc;
        }

        int version = Integer.parseInt(twoDDocContent.substring(2,4));
        if (version != 4) {
            return twoDDoc;
        }

        twoDDoc.setIDFlag(twoDDocContent.substring(0,2));
        twoDDoc.setVersion(version);
        twoDDoc.setIssuer(twoDDocContent.substring(4,8));
        twoDDoc.setCertId(twoDDocContent.substring(8, 12));
        twoDDoc.setDocumentDate(twoDDocContent.substring(12, 16));
        twoDDoc.setSignatureDate(twoDDocContent.substring(16, 20));
        twoDDoc.setDocumentTypeId(twoDDocContent.substring(20, 22));
        twoDDoc.setPerimeterId(twoDDocContent.substring(22, 24));
        twoDDoc.setCountryId(twoDDocContent.substring(24, 26));

        String remain = twoDDocContent.substring(26);
        String[] unitParts = remain.split(String.valueOf(ASCII_UNIT_SEPARATOR));
        String data = unitParts[0];
        String signature = unitParts[1];

        while (data.length() > 0) {
            int separatorLength = 0;
            String id = data.substring(0, 2);
            data = data.substring(2);
            TwoDDocIdEnum docId = TwoDDocIdEnum.valueOf("ID_"+id);
            if (docId.getMinSize() == docId.getMaxSize()) {
                twoDDoc.getData().put(id, data.substring(0, docId.getMaxSize()));
                data = data.substring(docId.getMaxSize());
            } else {
                String potentialResult = data;
                if (docId.getMaxSize() > 0) {
                    potentialResult = data.substring(0, docId.getMaxSize());
                }
                int pos = potentialResult.indexOf(ASCII_GROUP_SEPARATOR);
                if (pos < 0) {
                    pos = potentialResult.length();
                } else {
                    separatorLength = 1;
                }
                String result = data.substring(0, pos);
                data = data.substring(pos+separatorLength);
                twoDDoc.getData().put(id, result);
            }
        }

        return twoDDoc;
    }
}
