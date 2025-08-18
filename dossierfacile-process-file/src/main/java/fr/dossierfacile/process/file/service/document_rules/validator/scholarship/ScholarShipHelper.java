package fr.dossierfacile.process.file.service.document_rules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;

import java.util.Optional;

public class ScholarShipHelper {

    private ScholarShipHelper() {
        // Utility class, no instantiation needed
    }

    public static Optional<ScholarshipFile> getOptionalValue(Document document) {
        return document.getFiles().stream()
                .filter(file -> {
                    if (file.getParsedFileAnalysis() != null) {
                        return ParsedFileClassification.SCHOLARSHIP == file.getParsedFileAnalysis().getClassification();
                    }
                    return false;
                })
                .map(file -> (ScholarshipFile) file.getParsedFileAnalysis().getParsedFile())
                .findFirst();
    }

}
