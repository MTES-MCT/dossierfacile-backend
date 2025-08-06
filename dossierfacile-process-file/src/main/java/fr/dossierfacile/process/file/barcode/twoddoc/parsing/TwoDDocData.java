package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import fr.dossierfacile.process.file.util.TwoDDocUtil;

import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocC40Parser.ASCII_GROUP_SEPARATOR;

@Slf4j
public record TwoDDocData(Map<TwoDDocDataType, String> data) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static TwoDDocData parse(String rawData) {
        RawDataReader reader = new RawDataReader(rawData);
        Map<TwoDDocDataType, String> parsedData = new HashMap<>();

        while (reader.charactersRemaining()) {
            try {
                TwoDDocDataType dataType = reader.readDataId();
                String value = reader.readDataValue(dataType);
                if (dataType.equals(TwoDDocDataType.ID_53) || dataType.equals(TwoDDocDataType.ID_54)) {
                    value = TwoDDocUtil.getLocalDateFrom2DDocHexDate(value).format(DATE_TIME_FORMATTER);
                }
                parsedData.put(dataType, value);
            } catch (IllegalArgumentException e) {
                // Expected error: no enum constant TwoDDocDataType.ID_XX
                log.warn("Error parsing 2D-DOC", e);
            }
        }

        return new TwoDDocData(parsedData);
    }

    public Map<String, String> withLabels() {
        return data.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getLabel(),
                        Map.Entry::getValue));
    }

    @RequiredArgsConstructor
    private static class RawDataReader {

        private final String string;
        private int cursorIndex = 0;

        boolean charactersRemaining() {
            return string.length() > cursorIndex;
        }

        TwoDDocDataType readDataId() {
            String id = read(2);
            return TwoDDocDataType.of(id);
        }

        String readDataValue(TwoDDocDataType dataType) {
            if (dataType.hasFixedSize()) {
                return read(dataType.getMaxSize());
            }
            return readUntilSeparator();
        }

        private String read(int size) {
            String substring = string.substring(cursorIndex, cursorIndex + size);
            cursorIndex += size;
            return substring;
        }

        private String readUntilSeparator() {
            int characterIndex = string.substring(cursorIndex).indexOf(ASCII_GROUP_SEPARATOR);
            if (characterIndex > 0) {
                String value = read(characterIndex);
                cursorIndex += 1;
                return value;
            }
            return read(string.substring(cursorIndex).length());
        }

    }

    public String get(TwoDDocDataType type) {
        return data.get(type);
    }

}
