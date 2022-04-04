package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import java.io.IOException;
import java.io.InputStream;

public interface PdfTemplate<T> {
    InputStream render(T data) throws IOException;
}