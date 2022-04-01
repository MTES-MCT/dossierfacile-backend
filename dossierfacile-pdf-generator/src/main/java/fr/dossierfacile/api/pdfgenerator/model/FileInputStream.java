package fr.dossierfacile.api.pdfgenerator.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
@Builder
@Getter
public class FileInputStream {
    private final InputStream inputStream;
    private final String extension;
}
