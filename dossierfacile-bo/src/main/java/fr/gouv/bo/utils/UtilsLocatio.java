package fr.gouv.bo.utils;

import fr.gouv.bo.dto.AvgDTO;
import fr.gouv.bo.dto.CountActionsDTO;
import fr.gouv.bo.dto.CountDTO;
import fr.gouv.bo.dto.ErrorFieldDTO;
import fr.gouv.bo.dto.KeyStatistics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class UtilsLocatio {

    private UtilsLocatio() {
    }

    public static boolean isNumeric(String string) {
        Long longValue;
        System.out.println(String.format("Parsing string: \"%s\"", string));
        if (string == null || string.equals("")) {
            System.out.println("String cannot be parsed, it is null or empty.");
            return false;
        }
        try {
            longValue = Long.parseLong(string);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Input String cannot be parsed to Long.");
        }
        return false;
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

    public static void extractAvg(Map<KeyStatistics, Map<String, Double>> map, List<AvgDTO> list, String key) {
        for (AvgDTO avgDTO : list) {
            int year = avgDTO.getYear();
            int week = avgDTO.getWeek();
            KeyStatistics keyStatistics = new KeyStatistics(week, year);
            if (map.containsKey(keyStatistics)) {
                map.get(keyStatistics).put(key, avgDTO.getAvg());
            } else {
                Map<String, Double> m = new HashMap<>();
                m.put(key, avgDTO.getAvg());
                map.put(keyStatistics, m);
            }
        }
    }

    public static Map<KeyStatistics, Map<Integer, Long>> extractStatisticsActions(List<CountActionsDTO> objectList) {
        Map<KeyStatistics, Map<Integer, Long>> map = new HashMap<>();
        for (CountActionsDTO countActionsDTO : objectList) {
            int year = countActionsDTO.getYear();
            int week = countActionsDTO.getWeek();
            KeyStatistics keyStatistics = new KeyStatistics(week, year);
            if (map.containsKey(keyStatistics)) {
                map.get(keyStatistics).put(countActionsDTO.getActionType(), countActionsDTO.getCount());
            } else {
                Map<Integer, Long> m = new HashMap<>();
                m.put(countActionsDTO.getActionType(), countActionsDTO.getCount());
                map.put(keyStatistics, m);
            }
        }

        return map;
    }

    public static <T> List<T> sortedDates(List<T> arrayList) {
        arrayList.sort(Collections.reverseOrder());
        return arrayList;
    }

    public static double angle(double a, double b, double c) {
        return Math.acos((Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2 * a * b));
    }

    public static List<ErrorFieldDTO> errorFieldDTOS(BindingResult bindingResult) {
        List<ErrorFieldDTO> errorFieldDTOList = new ArrayList<>();
        for (ObjectError objectError : bindingResult.getAllErrors()) {
            FieldError fieldError = ((FieldError) objectError);
            errorFieldDTOList.add(new ErrorFieldDTO(fieldError.getField(), fieldError.getRejectedValue() != null ? Objects.requireNonNull(fieldError.getRejectedValue()).toString() : "", fieldError.getDefaultMessage()));
        }
        return errorFieldDTOList;
    }


    public static List<String> sortedDates(String date, int days) {
        int ini = 0;
        List<String> result = new ArrayList<>();
        while (days != ini) {
            LocalDate localDate = LocalDate.parse(date).minusDays(ini);
            result.add(localDate.toString());
            ini++;
        }
        return result;
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
