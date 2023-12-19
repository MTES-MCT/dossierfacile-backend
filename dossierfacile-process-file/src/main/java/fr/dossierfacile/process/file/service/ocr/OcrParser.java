package fr.dossierfacile.process.file.service.ocr;


import fr.dossierfacile.common.entity.ocr.ParsedFile;

import java.io.File;

public interface OcrParser<T extends ParsedFile> {
    T parse(File file);
}
