package fr.gouv.owner.utils;

import fr.gouv.owner.dto.CountDTO;
import fr.gouv.owner.model.KeyStatistics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class UtilsLocatio {

    private UtilsLocatio() {
    }

    public static String generateRandomString(int length) {

        String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
        String CHAR_UPPER = CHAR_LOWER.toUpperCase();
        String NUMBER = "0123456789";

        String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
        SecureRandom random = new SecureRandom();

        if (length < 1) throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);

            sb.append(rndChar);
        }
        return sb.toString();
    }


    public static byte[] convertImgToPDF(MultipartFile multipartFile) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PDDocument document = new PDDocument()) {
            BufferedImage bimg = ImageIO.read(multipartFile.getInputStream());
            if (bimg != null) {
                float width = bimg.getWidth();
                float height = bimg.getHeight();
                PDPage page = new PDPage(new PDRectangle(width, height));
                document.addPage(page);
                PDImageXObject img = PDImageXObject.createFromByteArray(document, multipartFile.getBytes(), multipartFile.getOriginalFilename());
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(img, 0, 0);
                contentStream.close();
                document.save(baos);
            }
        } catch (IOException e) {
            log.error("Exception white trying convert image to pdf", e);
        }
        return baos.toByteArray();
    }

    public static void extractStatistics(Map<KeyStatistics, Map<String, Long>> map, List<CountDTO> list, String key) {
        for (CountDTO countDTO : list) {
            int year = countDTO.getYear();
            int week = countDTO.getWeek();
            KeyStatistics keyStatistics = new KeyStatistics(week, year);
            if (map.containsKey(keyStatistics)) {
                map.get(keyStatistics).put(key, countDTO.getCount());
            } else {
                Map<String, Long> m = new HashMap<>();
                m.put(key, countDTO.getCount());
                map.put(keyStatistics, m);
            }
        }
    }

    public static <T> List<T> sortedDates(List<T> arrayList) {
        arrayList.sort(Collections.reverseOrder());
        return arrayList;
    }

    public static String date(int week, int year) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMM");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String initDate = sdf.format(cal.getTime());
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        String endDate = sdf.format(cal.getTime());
        return year + "/" + (week) + "  " + (initDate + "-" + endDate);
    }
}
