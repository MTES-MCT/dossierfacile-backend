package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import lombok.extern.slf4j.Slf4j;

import static fr.dossierfacile.common.enums.DocumentSubCategory.CERTIFICATE_VISA;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;

@Slf4j
public class FileParsingEligibilityCriteria {

    public static boolean shouldBeParse(File file) {
        if (file == null) {
            log.error("File cannot be empty dfFile");
            return false;
        }
        Document document = file.getDocument();
        return switch (document.getDocumentCategory()) {
            case TAX -> file.getDocument().getDocumentSubCategory() == MY_NAME;
            case IDENTIFICATION -> file.getDocument().getDocumentSubCategory() == CERTIFICATE_VISA;
            default -> false;
        };
    }
}
