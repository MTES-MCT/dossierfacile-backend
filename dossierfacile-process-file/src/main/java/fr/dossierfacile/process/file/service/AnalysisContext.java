package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisContext {
    private File dfFile;
    private BarCodeFileAnalysis barCodeFileAnalysis;
    private java.io.File file;
    private ParsedFile parsedDocument;
}
