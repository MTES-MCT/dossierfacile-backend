package fr.dossierfacile.common.entity.ocr;

import fr.dossierfacile.common.enums.ParsedFileClassification;

import java.io.Serializable;

public interface ParsedFile extends Serializable {
    ParsedFileClassification getClassification();
}