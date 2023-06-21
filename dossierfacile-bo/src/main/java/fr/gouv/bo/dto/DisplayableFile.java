package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class DisplayableFile {

    private final Long id;
    private final int order;
    private final DisplayableBarCodeFileAnalysis analysis;

    public static List<DisplayableFile> allOf(Document document) {
        List<File> files = document.getFiles();
        List<DisplayableFile> results = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            var analysis = DisplayableBarCodeFileAnalysis.of(file).orElse(null);
            results.add(new DisplayableFile(file.getId(), i + 1, analysis));
        }
        return results;
    }

    public static List<DisplayableFile> onlyAnalyzedFilesOf(Document document) {
        return allOf(document).stream()
                .filter(DisplayableFile::hasBeenAnalyzed)
                .collect(Collectors.toList());
    }

    public String getSummary() {
        String prefix = "Fichier n°";
        if (analysis == null) {
            return prefix + order;
        }
        return prefix + order + " (" + analysis.getIssuerName() + ") :";
    }

    public boolean hasBeenAnalyzed() {
        return analysis != null;
    }

}
