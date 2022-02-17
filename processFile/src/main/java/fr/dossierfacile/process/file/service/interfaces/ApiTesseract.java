package fr.dossierfacile.process.file.service.interfaces;

public interface ApiTesseract {
    String apiTesseract(String path, int[] pages, int dpi);
}
