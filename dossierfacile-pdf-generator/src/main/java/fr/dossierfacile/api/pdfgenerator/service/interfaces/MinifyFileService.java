package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.File;

import java.io.InputStream;

public interface MinifyFileService {
    void process(InputStream inputStream, File file);
}
