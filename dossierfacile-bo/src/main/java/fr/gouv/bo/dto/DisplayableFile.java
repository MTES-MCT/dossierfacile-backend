package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class DisplayableFile {

    private final Long id;
    private final int order;

    public static List<DisplayableFile> allOf(Document document) {
        List<File> files = document.getFiles();
        List<DisplayableFile> results = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            results.add(new DisplayableFile(file.getId(), i + 1));
        }
        return results;
    }

    public String getSummary() {
        String prefix = "Fichier n°";
        return prefix + order;
    }

}
