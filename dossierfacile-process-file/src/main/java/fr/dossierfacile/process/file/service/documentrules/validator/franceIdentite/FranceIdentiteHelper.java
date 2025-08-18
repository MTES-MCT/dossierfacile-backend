package fr.dossierfacile.process.file.service.documentrules.validator.franceIdentite;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.FranceIdentiteApiResult;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;

import java.util.Optional;

public class FranceIdentiteHelper {

    private FranceIdentiteHelper() {
        // Utility class, no instantiation needed
    }

    public static Optional<FranceIdentiteApiResult> getOptionalValue(Document document) {
        return document.getFiles().stream()
                .filter(file -> {
                    if (file.getParsedFileAnalysis() != null && file.getParsedFileAnalysis().getAnalysisStatus() == ParsedFileAnalysisStatus.COMPLETED) {
                        return ParsedFileClassification.FRANCE_IDENTITE == file.getParsedFileAnalysis().getClassification();
                    }
                    return false;
                })
                .map(file -> (FranceIdentiteApiResult) file.getParsedFileAnalysis().getParsedFile())
                .filter(parsedFile -> parsedFile != null
                        && !"ERROR_FILE".equals(parsedFile.getStatus())
                        && parsedFile.getStatus() != null
                        && parsedFile.getAttributes().getFamilyName() != null
                        && parsedFile.getAttributes().getGivenName() != null
                        && parsedFile.getAttributes().getValidityDate() != null)
                .findFirst();
    }
}