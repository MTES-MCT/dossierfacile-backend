package fr.dossierfacile.common.entity.ocr;

import fr.dossierfacile.common.enums.ParsedFileClassification;

public interface ParsedFile {
    ParsedFileClassification getClassification();
}