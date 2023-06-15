package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocC40Parser.ASCII_GROUP_SEPARATOR;

public record TwoDDocData(Map<TwoDDocDataType, String> data) {

    static TwoDDocData parse(String rawData) {
        RawDataReader reader = new RawDataReader(rawData);
        Map<TwoDDocDataType, String> parsedData = new HashMap<>();

        while (reader.charactersRemaining()) {
            TwoDDocDataType dataType = reader.readDataId();
            String value = reader.readDataValue(dataType);
            parsedData.put(dataType, value);
        }

        return new TwoDDocData(parsedData);
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
