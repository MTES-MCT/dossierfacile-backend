package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;

public class FileTypeUtil {

    private static final String PDF_TYPE = "application/pdf";

    public static boolean isPdf(File file) {
        return PDF_TYPE.equals(file.getStorageFile().getContentType());
    }

}
