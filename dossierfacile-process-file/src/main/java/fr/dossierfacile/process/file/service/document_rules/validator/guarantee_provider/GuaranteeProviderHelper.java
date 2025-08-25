package fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;

import java.util.Optional;

public class GuaranteeProviderHelper {

    private GuaranteeProviderHelper() {
        // Utility class, no instantiation
    }

    public static Optional<GuaranteeProviderFile> getGuaranteeProviderFile(Document document) {
        var files = document.getFiles();
        if (files == null || files.isEmpty()) {
            return Optional.empty();
        }
        var firstDocument = files.getFirst();
        if (firstDocument == null
                || firstDocument.getParsedFileAnalysis() == null
                || firstDocument.getParsedFileAnalysis().getParsedFile() == null) {
            return Optional.empty();
        }
        return Optional.of((GuaranteeProviderFile) firstDocument.getParsedFileAnalysis().getParsedFile());
    }

}
