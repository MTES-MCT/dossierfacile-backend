package fr.dossierfacile.api.pdfgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class Utility {

    public static void addParagraph(PDPageContentStream contentStream, float width, float sx, float sy,
                                    List<String> text, boolean justify, PDFont font, float fontSize) throws IOException {
        List<List<String>> listList = parseLines(text, width, font, fontSize);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(sx, sy);
        for (List<String> lines : listList
        ) {
            for (String line : lines) {
                float charSpacing = 0;
                if (justify && line.length() > 1) {
                    float size = fontSize * font.getStringWidth(line) / 1000;
                    float free = width - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }

                }
                contentStream.setCharacterSpacing(charSpacing);
                contentStream.showText(line);
                contentStream.newLine();
            }
        }
    }


    private static List<List<String>> parseLines(List<String> list, float width, PDFont font, float fontSize) throws IOException {
        List<List<String>> listArrayList = new ArrayList<>();
        for (String text : list
        ) {
            List<String> lines = new ArrayList<>();
            int lastSpace = -1;
            while (text.length() > 0) {
                int spaceIndex = text.indexOf(' ', lastSpace + 1);
                if (spaceIndex < 0)
                    spaceIndex = text.length();
                String subString = text.substring(0, spaceIndex);
                float size = fontSize * font.getStringWidth(subString) / 1000;
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
            }
            listArrayList.add(lines);
        }
        return listArrayList;
    }
}
