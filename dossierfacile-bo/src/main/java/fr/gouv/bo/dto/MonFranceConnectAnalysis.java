package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class MonFranceConnectAnalysis {

    private final Map<Integer, QrCodeFileAnalysis> results;

    public static MonFranceConnectAnalysis of(List<File> files) {
        Map<Integer, QrCodeFileAnalysis> results = new HashMap<>();
        for (int i = 0; i < files.size(); i++) {
            QrCodeFileAnalysis result = files.get(i).getFileAnalysis();
            if (result != null) {
                results.put(i + 1, result);
            }
        }
        return new MonFranceConnectAnalysis(results);
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

}
