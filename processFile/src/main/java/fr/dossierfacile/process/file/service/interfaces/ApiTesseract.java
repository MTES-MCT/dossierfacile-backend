package fr.dossierfacile.process.file.service.interfaces;

import java.util.List;

public interface ApiTesseract {
    String apiTesseract(List<String> path, int[] pages, int dpi);
}
