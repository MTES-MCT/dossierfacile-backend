package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.MonFranceConnectValidationResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class MonFranceConnectAnalysis {

    private final Map<Integer, MonFranceConnectValidationResult> results;

    public static MonFranceConnectAnalysis of(List<File> files) {
        Map<Integer, MonFranceConnectValidationResult> results = new HashMap<>();
        for (int i = 0; i < files.size(); i++) {
            MonFranceConnectValidationResult result = files.get(i).getMfcValidationResult();
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
