package fr.dossierfacile.api.pdfgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Utility {

    public static void addText(PDPageContentStream contentStream, float width, float sx, float sy,
                               String text, PDType0Font font, float fontSize, PDType0Font alternativeFont) throws IOException {
        text = StringUtils.trim(text);

        String[] paragraphs = text.split("[\\r\\n]+");
        contentStream.setFont(alternativeFont, fontSize);
        contentStream.beginText();
        contentStream.newLineAtOffset(sx, sy);
        contentStream.newLine();
        for (String paragraph : paragraphs) {
            List<String> lines = sentanceToLines(paragraph, width, font, fontSize, alternativeFont);
            for (int i = 0; i < lines.size(); i++) {
                contentStream.newLine();
                float charSpacing = 0;
                List<String> results = Pattern.compile("\\P{M}\\p{M}*+").matcher(lines.get(i))
                        .results()
                        .map(MatchResult::group)
                        .collect(Collectors.toList());
                if (i < lines.size() - 1) {
                    float size = getLineWidth(results, font, alternativeFont, fontSize);
                    float free = width - size;
                    charSpacing = free / (lines.get(i).length() - 1);
                    contentStream.setCharacterSpacing(charSpacing);
                }
                for (String c : results) {
                    if (isFontSupported(c, font)) {
                        contentStream.setFont(font, fontSize);
                    } else if (isFontSupported(c, alternativeFont)) {
                        contentStream.setFont(alternativeFont, fontSize);
                    } else {
                        continue;
                    }
                    contentStream.showText(c);
                }
            }
        }
        contentStream.endText();
    }

    public static boolean isFontSupported(String text, PDType0Font font) {
        try {
            font.encode(text);
            return true;
        } catch (Exception swallow) {
            return false;
        }
    }

    private static float getLineWidth(List<String> chars, PDType0Font font1, PDType0Font font2, float fontSize) {
        return (float) chars.parallelStream().map(s -> {
            try {
                if (isFontSupported(s, font1)) {
                    return fontSize * font1.getStringWidth(s) / 1000;
                } else if (isFontSupported(s, font2)) {
                    return fontSize * font2.getStringWidth(s) / 1000;
                } else {
                    return 0;
                }
            } catch (Exception e) {
                return 0;
            }
        }).mapToDouble(Number::doubleValue).sum();
    }

    public static List<String> sentanceToLines(String text, float width, PDType0Font font, float fontSize, PDType0Font alternativeFont) {
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            List<String> chars = Pattern.compile("\\P{M}\\p{M}*+").matcher(subString)
                    .results()
                    .map(MatchResult::group)
                    .collect(Collectors.toList());
            try {
                float size = getLineWidth(chars, font, alternativeFont, fontSize);
                if (size > width) {
                    if (lastSpace < 0) {
                        lastSpace = spaceIndex;
                    }
                    subString = text.substring(0, lastSpace);
                    lines.add(subString);
                    text = text.substring(lastSpace).trim();
                    lastSpace = -1;
                } else if (spaceIndex == text.length()) {
                    lines.add(text);
                    text = "";
                } else {
                    lastSpace = spaceIndex;
                }
            } catch (IllegalArgumentException e) {
                text = text.substring(0, lastSpace).trim();
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

}
