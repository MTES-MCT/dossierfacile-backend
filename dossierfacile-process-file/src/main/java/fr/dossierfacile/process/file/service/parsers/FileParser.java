package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.ParsedFile;
import java.io.File;

public interface FileParser<T extends ParsedFile> {
    T parse(File file);
}